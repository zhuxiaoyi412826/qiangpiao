package com.qiangpiao.service.impl;

import com.qiangpiao.bo.RangeBO;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.dataobject.TrainSegmentStockDO;
import com.qiangpiao.dataobject.TrainStopDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.mapper.SeatSegmentMapper;
import com.qiangpiao.mapper.TrainMapper;
import com.qiangpiao.mapper.TrainSegmentStockMapper;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.SegmentStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 区间票库存实现。
 *
 * <pre>
 *   余票：OD 区间覆盖的每一段取最小值（木桶效应），Redis 预热优先、DB 兜底
 *   占用：一段 UPDATE 覆盖所有段，靠 available_count &gt;= n 的行条件防超卖
 *   兼容：表未建 / 车次无时刻表 → 全程票，行为与改造前一致
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SegmentStockServiceImpl implements SegmentStockService {

    /** Redis 区间库存预热时长（分钟） */
    private static final long SEG_TTL_MINUTES = 120;

    private final TrainSegmentStockMapper segmentStockMapper;
    private final SeatSegmentMapper seatSegmentMapper;
    private final TrainMapper trainMapper;
    private final TrainStockMapper trainStockMapper;
    private final StringRedisTemplate stringRedisTemplate;
    @org.springframework.beans.factory.annotation.Qualifier("seckillSegStockScript")
    private final org.springframework.data.redis.core.script.RedisScript<Long> seckillSegStockScript;
    @org.springframework.beans.factory.annotation.Qualifier("rollbackSegStockScript")
    private final org.springframework.data.redis.core.script.RedisScript<Long> rollbackSegStockScript;

    /** 建表探测结果缓存：null 未探测，TRUE 可用，FALSE 未建表（降级为全程票） */
    private volatile Boolean enabled;

    @Override
    public boolean enabled() {
        Boolean value = enabled;
        if (value != null) {
            return value;
        }
        synchronized (this) {
            if (enabled == null) {
                try {
                    segmentStockMapper.countByTrainId(0L);
                    seatSegmentMapper.countConflict(0L, 1, 1);
                    enabled = Boolean.TRUE;
                    log.info("区间票能力已启用（t_train_segment_stock / t_seat_segment 均已建表）");
                } catch (Exception e) {
                    enabled = Boolean.FALSE;
                    log.warn("区间票表尚未创建，退化为全程票模式：{}", e.getMessage());
                }
            }
            return enabled;
        }
    }

    @Override
    public RangeBO resolveRange(Long trainId, String fromStation, String toStation) {
        List<TrainStopDO> stops = listStops(trainId);
        if (stops.size() < 2 || !enabled()) {
            return RangeBO.builder()
                    .fromOrder(RangeBO.DEFAULT_FROM)
                    .toOrder(RangeBO.DEFAULT_TO)
                    .build();
        }
        List<Integer> orders = new ArrayList<>();
        for (TrainStopDO stop : stops) {
            if (stop.getStopOrder() != null) {
                orders.add(stop.getStopOrder());
            }
        }
        int first = orders.isEmpty() ? 1 : Collections.min(orders);
        int last = orders.isEmpty() ? stops.size() : Collections.max(orders);

        int fromOrder = first;
        int toOrder = last;
        String fromName = null;
        String toName = null;
        if (StringUtils.hasText(fromStation)) {
            Integer hit = firstOrderByName(stops, fromStation);
            if (hit != null) {
                fromOrder = hit;
                fromName = fromStation;
            }
        }
        if (StringUtils.hasText(toStation)) {
            Integer hit = firstOrderAfter(stops, toStation, fromOrder);
            if (hit != null) {
                toOrder = hit;
                toName = toStation;
            }
        }
        if (toOrder <= fromOrder) {
            // 下车站必须晚于上车站，否则按全程处理
            fromOrder = first;
            toOrder = last;
        }
        if (fromName == null) {
            fromName = stationNameAt(stops, fromOrder);
        }
        if (toName == null) {
            toName = stationNameAt(stops, toOrder);
        }
        return RangeBO.builder()
                .fromOrder(fromOrder)
                .toOrder(toOrder)
                .fromStationName(fromName)
                .toStationName(toName)
                .build();
    }

    @Override
    public Integer available(Long trainId, Integer seatType, RangeBO range) {
        if (!enabled() || range == null) {
            return null;
        }
        List<String> keys = stockKeys(trainId, seatType, range);
        try {
            List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);
            if (values != null && !values.isEmpty() && values.stream().allMatch(StringUtils::hasText)) {
                int min = values.stream()
                        .map(v -> {
                            try {
                                return Integer.parseInt(v);
                            } catch (NumberFormatException e) {
                                return 0;
                            }
                        })
                        .min(Integer::compareTo).orElse(0);
                return Math.max(min, 0);
            }
        } catch (Exception e) {
            log.warn("读取 Redis 区间余票失败，改用 DB：trainId={}, msg={}", trainId, e.getMessage());
        }
        try {
            return segmentStockMapper.minAvailable(trainId, seatType, range.getFromOrder(), range.getToOrder());
        } catch (Exception e) {
            log.warn("读取 DB 区间余票失败：trainId={}, msg={}", trainId, e.getMessage());
            return null;
        }
    }

    @Override
    public List<String> stockKeys(Long trainId, Integer seatType, RangeBO range) {
        List<String> keys = new ArrayList<>();
        if (range == null || range.getFromOrder() == null || range.getToOrder() == null) {
            return keys;
        }
        for (int i = range.getFromOrder(); i < range.getToOrder(); i++) {
            keys.add(RedisKeys.seckillSegStock(trainId, seatType, i));
        }
        return keys;
    }

    @Override
    public int deductRedis(Long trainId, Integer seatType, RangeBO range, int count) {
        List<String> keys = stockKeys(trainId, seatType, range);
        if (keys.isEmpty()) {
            return -1;
        }
        try {
            Long result = stringRedisTemplate.execute(seckillSegStockScript, keys, String.valueOf(count));
            return result == null ? -1 : result.intValue();
        } catch (Exception e) {
            log.warn("区间库存 Redis 扣减失败：trainId={}, msg={}", trainId, e.getMessage());
            return -1;
        }
    }

    @Override
    public void rollbackRedis(Long trainId, Integer seatType, RangeBO range, int count) {
        List<String> keys = stockKeys(trainId, seatType, range);
        if (keys.isEmpty()) {
            return;
        }
        try {
            stringRedisTemplate.execute(rollbackSegStockScript, keys, String.valueOf(count));
        } catch (Exception e) {
            log.error("区间库存 Redis 回滚失败：trainId={}, msg={}", trainId, e.getMessage());
        }
    }

    @Override
    public boolean occupy(Long trainId, Integer seatType, RangeBO range, int count) {
        if (!enabled() || range == null) {
            return true;
        }
        int expected = range.segmentCount();
        int rows = segmentStockMapper.decreaseRange(trainId, seatType,
                range.getFromOrder(), range.getToOrder(), count);
        if (rows != expected) {
            log.warn("区间库存扣减不完整：trainId={}, seatType={}, 区间=[{},{}), 期望={} 段，实际={} 段",
                    trainId, seatType, range.getFromOrder(), range.getToOrder(), expected, rows);
            return false;
        }
        return true;
    }

    @Override
    public void release(Long trainId, Integer seatType, RangeBO range, int count) {
        if (!enabled() || range == null) {
            return;
        }
        try {
            segmentStockMapper.increaseRange(trainId, seatType,
                    range.getFromOrder(), range.getToOrder(), count);
        } catch (Exception e) {
            log.error("区间库存归还失败：trainId={}, seatType={}, msg={}", trainId, seatType, e.getMessage());
        }
    }

    @Override
    public int initSegments(Long trainId) {
        if (!enabled()) {
            return 0;
        }
        List<TrainStockDO> stocks = trainStockMapper.selectByTrainId(trainId);
        if (stocks == null || stocks.isEmpty()) {
            return 0;
        }
        int stopCount = Math.max(stopCount(trainId), 2);
        int created = 0;
        for (TrainStockDO stock : stocks) {
            Integer seatType = stock.getSeatType();
            /** 已分配的段价累计：用于最后一段做尾差调整，保证「各段之和 = 席别全程价」 */
            BigDecimal allocated = BigDecimal.ZERO;
            for (int seg = 1; seg < stopCount; seg++) {
                TrainSegmentStockDO exist = firstSegment(trainId, seatType, seg);
                int available = exist == null || exist.getAvailableCount() == null
                        ? (stock.getAvailableCount() == null ? 0 : stock.getAvailableCount())
                        : exist.getAvailableCount();
                int total = stock.getTotalCount() == null ? 0 : stock.getTotalCount();
                TrainSegmentStockDO segStock = new TrainSegmentStockDO();
                segStock.setTrainId(trainId);
                segStock.setSeatType(seatType);
                segStock.setSegIndex(seg);
                segStock.setTotalCount(total);
                segStock.setAvailableCount(available);
                // 段价：按里程比例把席别全程价拆到这一段；已人工定过价的段不会被覆盖
                BigDecimal segPrice = segPriceOf(trainId, seg, stock.getPrice());
                // 尾差调整：最后一段吃掉前面四舍五入的零头，保证买全程仍是原价（否则会多收 / 少收几分钱，
                // 与「资金对账」的应有金额对不上）
                if (seg == stopCount - 1 && segPrice != null && stock.getPrice() != null) {
                    BigDecimal rest = stock.getPrice().subtract(allocated);
                    segPrice = rest.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rest;
                }
                if (segPrice != null) {
                    allocated = allocated.add(segPrice);
                }
                segStock.setPrice(segPrice);
                try {
                    segmentStockMapper.upsert(segStock);
                    if (exist != null && exist.getPrice() == null && segPrice != null) {
                        segmentStockMapper.updatePriceIfNull(trainId, seatType, seg, segPrice);
                    }
                    // Redis 预热：已存在的不覆盖，避免把已售出的票数刷回去
                    String key = RedisKeys.seckillSegStock(trainId, seatType, seg);
                    stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(available),
                            SEG_TTL_MINUTES, TimeUnit.MINUTES);
                    created++;
                } catch (Exception e) {
                    log.warn("初始化区间库存失败：trainId={}, seatType={}, seg={}, msg={}",
                            trainId, seatType, seg, e.getMessage());
                }
            }
        }
        log.info("区间库存初始化完成：trainId={}, 席别={}, 段数={}", trainId, stocks.size(), created);
        return created;
    }

    @Override
    public int stopCount(Long trainId) {
        return listStops(trainId).size();
    }

    @Override
    public BigDecimal fare(Long trainId, Integer seatType, RangeBO range, BigDecimal fullPrice) {
        BigDecimal fallback = fullPrice == null ? BigDecimal.ZERO : fullPrice;
        if (!enabled() || range == null || range.getFromOrder() == null || range.getToOrder() == null
                || range.isFullRange()) {
            return fallback;
        }
        try {
            BigDecimal sum = segmentStockMapper.sumPrice(trainId, seatType, range.getFromOrder(), range.getToOrder());
            // SUM 为 NULL = 区间内缺段数据或至少有一段没定价，交给下面的比例折算
            if (sum != null && sum.compareTo(BigDecimal.ZERO) > 0) {
                return sum.setScale(2, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("读取段价失败，改用里程比例折算：trainId={}, seatType={}, msg={}", trainId, seatType, e.getMessage());
        }
        return ratioFare(trainId, range, fallback);
    }

    @Override
    public int updateSegmentPrice(Long trainId, Integer seatType, Integer segIndex, BigDecimal price) {
        if (!enabled()) {
            return 0;
        }
        return segmentStockMapper.updatePrice(trainId, seatType, segIndex, price);
    }

    @Override
    public List<TrainSegmentStockDO> listSegments(Long trainId) {
        if (!enabled()) {
            return Collections.emptyList();
        }
        try {
            List<TrainSegmentStockDO> list = segmentStockMapper.selectByTrainId(trainId);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.warn("查询分段库存失败：trainId={}, msg={}", trainId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 段价未维护时的降级口径：按里程比例折算全程价。
     * 缺里程时退化为「段数 / 总段数」，再算不出就退回全程价——宁可按原价卖，也不会算出 0 元单。
     */
    private BigDecimal ratioFare(Long trainId, RangeBO range, BigDecimal fullPrice) {
        if (fullPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return fullPrice;
        }
        List<TrainStopDO> stops = listStops(trainId);
        if (stops.size() < 2) {
            return fullPrice;
        }
        int first = firstStopOrder(stops);
        int last = lastStopOrder(stops);
        // 全程区间直接按席别原价：比例折算会有分位误差，导致「买全程」比对账的应有金额差几分钱
        if (range.getFromOrder() == first && range.getToOrder() == last) {
            return fullPrice;
        }
        double ratio;
        Integer fromDist = distanceAt(stops, range.getFromOrder());
        Integer toDist = distanceAt(stops, range.getToOrder());
        Integer firstDist = distanceAt(stops, first);
        Integer lastDist = distanceAt(stops, last);
        if (fromDist != null && toDist != null && firstDist != null && lastDist != null && lastDist > firstDist) {
            ratio = (double) (toDist - fromDist) / (lastDist - firstDist);
        } else if (last > first) {
            ratio = (double) (range.getToOrder() - range.getFromOrder()) / (last - first);
        } else {
            return fullPrice;
        }
        ratio = Math.max(0d, Math.min(1d, ratio));
        if (ratio <= 0d) {
            return fullPrice;
        }
        return fullPrice.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
    }

    /** 初始化段价：按里程比例把全程价拆到每一段（人工已定价的段在调用方跳过，不覆盖） */
    private BigDecimal segPriceOf(Long trainId, int seg, BigDecimal fullPrice) {
        if (fullPrice == null || fullPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return fullPrice;
        }
        List<TrainStopDO> stops = listStops(trainId);
        int first = firstStopOrder(stops);
        int last = lastStopOrder(stops);
        double ratio;
        Integer segStart = distanceAt(stops, seg);
        Integer segEnd = distanceAt(stops, seg + 1);
        Integer firstDist = distanceAt(stops, first);
        Integer lastDist = distanceAt(stops, last);
        if (segStart != null && segEnd != null && firstDist != null && lastDist != null && lastDist > firstDist) {
            ratio = (double) (segEnd - segStart) / (lastDist - firstDist);
        } else if (last > first) {
            ratio = 1d / (last - first);
        } else {
            return fullPrice;
        }
        ratio = Math.max(0d, Math.min(1d, ratio));
        if (ratio <= 0d) {
            return fullPrice;
        }
        return fullPrice.multiply(BigDecimal.valueOf(ratio)).setScale(2, RoundingMode.HALF_UP);
    }

    private int firstStopOrder(List<TrainStopDO> stops) {
        int min = Integer.MAX_VALUE;
        for (TrainStopDO stop : stops) {
            if (stop.getStopOrder() != null && stop.getStopOrder() < min) {
                min = stop.getStopOrder();
            }
        }
        return min == Integer.MAX_VALUE ? 1 : min;
    }

    private int lastStopOrder(List<TrainStopDO> stops) {
        int max = 0;
        for (TrainStopDO stop : stops) {
            if (stop.getStopOrder() != null && stop.getStopOrder() > max) {
                max = stop.getStopOrder();
            }
        }
        return max == 0 ? stops.size() : max;
    }

    /** 站序对应的累计里程；缺失返回 null（调用方据此降级为站序比例） */
    private Integer distanceAt(List<TrainStopDO> stops, int order) {
        for (TrainStopDO stop : stops) {
            if (stop.getStopOrder() != null && stop.getStopOrder() == order) {
                return stop.getDistanceKm();
            }
        }
        return null;
    }

    private List<TrainStopDO> listStops(Long trainId) {
        try {
            List<TrainStopDO> stops = trainMapper.listStops(trainId);
            return stops == null ? Collections.emptyList() : stops;
        } catch (Exception e) {
            log.warn("查询时刻表失败：trainId={}, msg={}", trainId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private Integer firstOrderByName(List<TrainStopDO> stops, String stationName) {
        for (TrainStopDO stop : stops) {
            if (stationName.equals(stop.getStationName()) && stop.getStopOrder() != null) {
                return stop.getStopOrder();
            }
        }
        return null;
    }

    /** 找下车站：必须晚于上车站，取第一个满足的站序 */
    private Integer firstOrderAfter(List<TrainStopDO> stops, String stationName, int fromOrder) {
        Integer hit = null;
        for (TrainStopDO stop : stops) {
            if (stationName.equals(stop.getStationName()) && stop.getStopOrder() != null
                    && stop.getStopOrder() > fromOrder) {
                hit = stop.getStopOrder();
                break;
            }
        }
        return hit;
    }

    private String stationNameAt(List<TrainStopDO> stops, int order) {
        for (TrainStopDO stop : stops) {
            if (stop.getStopOrder() != null && stop.getStopOrder() == order) {
                return stop.getStationName();
            }
        }
        return null;
    }

    private TrainSegmentStockDO firstSegment(Long trainId, Integer seatType, int seg) {
        try {
            List<TrainSegmentStockDO> list = segmentStockMapper.selectRange(trainId, seatType, seg, seg + 1);
            return list == null || list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }
}

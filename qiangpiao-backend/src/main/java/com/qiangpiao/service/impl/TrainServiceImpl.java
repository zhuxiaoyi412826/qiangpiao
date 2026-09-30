package com.qiangpiao.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.qiangpiao.bo.RangeBO;
import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.bo.TrainStockBO;
import com.qiangpiao.cache.MultiLevelCacheService;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.dto.TrainQueryDTO;
import com.qiangpiao.mapper.TrainMapper;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.service.SegmentStockService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.SeatMapVO;
import com.qiangpiao.vo.TrainDetailVO;
import com.qiangpiao.vo.TrainStockVO;
import com.qiangpiao.vo.TrainVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车次服务实现：车次查询 / 详情走三级缓存，余票优先使用 Redis 实时库存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainServiceImpl implements TrainService {

    private final TrainMapper trainMapper;
    private final TrainStockMapper trainStockMapper;
    private final SeatService seatService;
    private final MultiLevelCacheService cacheService;
    private final StringRedisTemplate stringRedisTemplate;
    private final com.qiangpiao.service.PurchaseLimitService purchaseLimitService;
    private final SegmentStockService segmentStockService;

    @Value("${cache.default-ttl}")
    private long cacheTtl;

    /** 可查询日期范围（天）：今天起 N 天内 */
    @Value("${ticket.query-days}")
    private int queryDays;

    /** 预售期（天）：只能预售今天起 N 天内的车票 */
    @Value("${ticket.presale-days}")
    private int presaleDays;

    /** 开车前多少分钟停止售票 */
    @Value("${ticket.stop-sell-minutes}")
    private long stopSellMinutes;

    @Override
    public PageResult<TrainVO> query(TrainQueryDTO queryDTO) {
        LocalDate today = LocalDate.now();
        LocalDate departDate = queryDTO.getDepartDate();
        if (departDate == null) {
            // 未指定日期时查当天，保证默认结果都是可买的车次
            departDate = today;
            queryDTO.setDepartDate(today);
        } else if (departDate.isBefore(today) || departDate.isAfter(today.plusDays(queryDays - 1L))) {
            throw new BizException(ResultCode.TRAIN_QUERY_DATE_INVALID);
        }

        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();
        String from = StringUtils.hasText(queryDTO.getFromStation()) ? queryDTO.getFromStation() : "";
        String to = StringUtils.hasText(queryDTO.getToStation()) ? queryDTO.getToStation() : "";
        String date = queryDTO.getDepartDate() == null ? "" : queryDTO.getDepartDate().toString();

        String key = RedisKeys.trainList(from, to, date) + ":" + pageNum + ":" + pageSize;
        PageResult<TrainVO> result = cacheService.get(
                key,
                new TypeReference<PageResult<TrainVO>>() {
                },
                () -> queryDb(queryDTO, pageNum, pageSize),
                cacheTtl);
        return result == null ? PageResult.of(pageNum, pageSize, 0, new ArrayList<>()) : result;
    }

    @Override
    public void assertTicketSellable(Long trainId) {
        TrainBO train = getTrainBO(trainId);
        if (!train.onSale()) {
            throw new BizException(ResultCode.TRAIN_NOT_SALE);
        }
        LocalDateTime now = LocalDateTime.now();
        // 售卖时间窗口（t_train.sale_start_time / sale_end_time，两列都为空表示不限制）
        if (train.getSaleStartTime() != null && now.isBefore(train.getSaleStartTime())) {
            throw new BizException(ResultCode.SALE_NOT_START);
        }
        if (train.getSaleEndTime() != null && now.isAfter(train.getSaleEndTime())) {
            throw new BizException(ResultCode.SALE_ENDED);
        }
        LocalDate today = now.toLocalDate();
        LocalDate departDate = train.getDepartDate();
        if (departDate == null || train.departed(now)) {
            throw new BizException(ResultCode.TRAIN_DEPARTED);
        }
        if (departDate.isAfter(today.plusDays(presaleDays))) {
            throw new BizException(ResultCode.TICKET_NOT_ON_SALE);
        }
        if (train.minutesToDepart(now) < stopSellMinutes) {
            throw new BizException(ResultCode.TICKET_STOP_SELL);
        }
    }

    @Override
    public String buyBlockReason(Long userId, Long trainId) {
        if (userId == null) {
            return null;
        }
        return purchaseLimitService.buyBlockReason(userId, getTrainBO(trainId));
    }

    @Override
    public List<com.qiangpiao.dataobject.TrainStopDO> stops(Long trainId) {
        return trainMapper.listStops(trainId);
    }

    @Override
    public int rollExpiredTrains() {
        LocalDate today = LocalDate.now();
        int rows = trainMapper.rollExpiredTrains(today);
        if (rows > 0) {
            // 日期变了：车次列表（按日期分 key）与详情（按 id）缓存全部失效
            cacheService.evictPrefix(Constants.CACHE_TRAIN_LIST);
            cacheService.evictPrefix(Constants.CACHE_TRAIN_DETAIL);
            log.info("车次日期滚动完成：{} 趟车次发车日期更新为 {}", rows, today);
        }
        return rows;
    }

    @Override
    public TrainDetailVO detail(Long trainId) {
        return detail(trainId, null, null);
    }

    public TrainDetailVO detail(Long trainId, String fromStation, String toStation) {
        TrainBO trainBO = getTrainBO(trainId);
        // 区间票：座位图与余票都按「本次乘车区间」算，其它区间被占的座位仍可选
        RangeBO range = segmentStockService.enabled()
                ? segmentStockService.resolveRange(trainId, fromStation, toStation)
                : null;
        List<SeatMapVO> seatMaps = new ArrayList<>();
        if (trainBO.getStocks() != null) {
            for (TrainStockBO stock : trainBO.getStocks()) {
                seatMaps.add(range == null
                        ? seatService.seatMap(trainId, stock.getSeatType())
                        : seatService.seatMap(trainId, stock.getSeatType(), range));
            }
        }
        TrainVO trainVO = toVO(trainBO, segmentAvailable(trainBO, fromStation, toStation));
        TrainDetailVO detail = new TrainDetailVO();
        org.springframework.beans.BeanUtils.copyProperties(trainVO, detail);
        detail.setSeatMaps(seatMaps);
        return detail;
    }

    @Override
    public TrainBO getTrainBO(Long trainId) {
        TrainBO bo = cacheService.get(RedisKeys.trainDetail(trainId), TrainBO.class,
                () -> loadTrainBO(trainId), cacheTtl);
        if (bo == null) {
            throw new BizException(ResultCode.TRAIN_NOT_FOUND);
        }
        return bo;
    }

    @Override
    public void evictTrainCache(Long trainId) {
        cacheService.evict(RedisKeys.trainDetail(trainId));
        // 列表页缓存按前缀清理代价高，这里依赖短 TTL 自动过期
        log.debug("失效车次缓存 trainId={}", trainId);
    }

    // ==================== private ====================

    private PageResult<TrainVO> queryDb(TrainQueryDTO queryDTO, int pageNum, int pageSize) {
        long offset = (long) (pageNum - 1) * pageSize;
        String from = StringUtils.hasText(queryDTO.getFromStation()) ? queryDTO.getFromStation() : null;
        String to = StringUtils.hasText(queryDTO.getToStation()) ? queryDTO.getToStation() : null;

        List<TrainDO> trains;
        long total;
        if (from == null && to == null && queryDTO.getDepartDate() == null) {
            trains = trainMapper.selectAll(offset, (long) pageSize);
            total = trainMapper.countAll();
        } else if (segmentStockService.enabled() && from != null && to != null) {
            // 区间票：按经停站匹配，支持「中途上车 / 中途下车」
            trains = trainMapper.selectBySegment(from, to, queryDTO.getDepartDate(), offset, (long) pageSize);
            total = trainMapper.countBySegment(from, to, queryDTO.getDepartDate());
            if (total == 0) {
                // 车次没有时刻表数据时回退到「始发站 → 终点站」精确匹配
                trains = trainMapper.selectByRoute(from, to, queryDTO.getDepartDate(), offset, (long) pageSize);
                total = trainMapper.countByRoute(from, to, queryDTO.getDepartDate());
            }
        } else {
            trains = trainMapper.selectByRoute(from, to, queryDTO.getDepartDate(), offset, (long) pageSize);
            total = trainMapper.countByRoute(from, to, queryDTO.getDepartDate());
        }
        List<TrainVO> list = trains.stream()
                .map(train -> {
                    TrainBO bo = loadTrainBO(train.getId());
                    // 余票按「本次查询的乘车区间」算：同座位分段售卖后，不同区间余票不同
                    return toVO(bo, segmentAvailable(bo, from, to));
                })
                .collect(Collectors.toList());
        return PageResult.of(pageNum, pageSize, total, list);
    }

    /**
     * 按乘车区间算各席别余票：区间余票 = 该区间覆盖的每一段余票的最小值。
     * 区间能力未开启 / 该区间无段数据时返回空 Map（调用方沿用全程库存，不污染缓存里的 BO）。
     */
    private java.util.Map<Integer, Integer> segmentAvailable(TrainBO bo, String from, String to) {
        java.util.Map<Integer, Integer> result = new java.util.HashMap<>();
        if (bo == null || bo.getStocks() == null || bo.getStocks().isEmpty()
                || !segmentStockService.enabled()) {
            return result;
        }
        RangeBO range = segmentStockService.resolveRange(bo.getId(), from, to);
        if (range == null || range.getFromOrder() == null || range.getToOrder() == null) {
            return result;
        }
        for (TrainStockBO stock : bo.getStocks()) {
            Integer available = segmentStockService.available(bo.getId(), stock.getSeatType(), range);
            if (available != null) {
                result.put(stock.getSeatType(), available);
            }
        }
        return result;
    }

    private TrainBO loadTrainBO(Long trainId) {
        TrainDO train = trainMapper.selectById(trainId);
        if (train == null) {
            return null;
        }
        List<TrainStockBO> stocks = trainStockMapper.selectByTrainId(trainId).stream()
                .map(stock -> TrainStockBO.builder()
                        .id(stock.getId())
                        .trainId(stock.getTrainId())
                        .seatType(stock.getSeatType())
                        .seatTypeName(seatTypeName(stock.getSeatType()))
                        .totalCount(stock.getTotalCount())
                        .availableCount(resolveAvailableCount(stock))
                        .price(stock.getPrice())
                        .version(stock.getVersion())
                        .build())
                .sorted(Comparator.comparing(TrainStockBO::getSeatType))
                .collect(Collectors.toList());

        return TrainBO.builder()
                .id(train.getId())
                .trainNo(train.getTrainNo())
                .trainType(train.getTrainType())
                .fromStationId(train.getFromStationId())
                .fromStationName(train.getFromStationName())
                .toStationId(train.getToStationId())
                .toStationName(train.getToStationName())
                .departDate(train.getDepartDate())
                .departTime(train.getDepartTime())
                .arriveTime(train.getArriveTime())
                .durationMinutes(train.getDurationMinutes())
                .status(train.getStatus())
                .saleStartTime(train.getSaleStartTime())
                .saleEndTime(train.getSaleEndTime())
                .stocks(stocks)
                .build();
    }

    /**
     * Redis 中已预热秒杀库存时，以 Redis 实时余票为准
     */
    private int resolveAvailableCount(TrainStockDO stock) {
        try {
            String value = stringRedisTemplate.opsForValue()
                    .get(RedisKeys.seckillStock(stock.getTrainId(), stock.getSeatType()));
            if (StringUtils.hasText(value)) {
                return Math.max(Integer.parseInt(value), 0);
            }
        } catch (Exception e) {
            log.warn("读取 Redis 余票失败，使用 DB 余票 trainId={}", stock.getTrainId());
        }
        return stock.getAvailableCount() == null ? 0 : stock.getAvailableCount();
    }

    private TrainVO toVO(TrainBO bo) {
        return toVO(bo, java.util.Collections.emptyMap());
    }

    /**
     * @param segAvailable 乘车区间余票（席别 -> 余票）；为空的席别沿用全程库存
     */
    private TrainVO toVO(TrainBO bo, java.util.Map<Integer, Integer> segAvailable) {
        List<TrainStockVO> stockVOs = bo.getStocks() == null ? new ArrayList<>() : bo.getStocks().stream()
                .map(stock -> TrainStockVO.builder()
                        .seatType(stock.getSeatType())
                        .seatTypeName(stock.getSeatTypeName())
                        .price(stock.getPrice())
                        .availableCount(segAvailable.getOrDefault(stock.getSeatType(),
                                stock.getAvailableCount()))
                        .totalCount(stock.getTotalCount())
                        .build())
                .collect(Collectors.toList());

        String sellTip = sellTip(bo, LocalDateTime.now());

        return TrainVO.builder()
                .id(bo.getId())
                .trainNo(bo.getTrainNo())
                .trainType(bo.getTrainType())
                .fromStationName(bo.getFromStationName())
                .toStationName(bo.getToStationName())
                .departDate(bo.getDepartDate())
                .departTime(bo.getDepartTime())
                .arriveTime(bo.getArriveTime())
                .durationText(bo.durationText())
                .status(bo.getStatus())
                .stocks(stockVOs)
                .sellable(sellTip == null)
                .sellTip(sellTip)
                .build();
    }

    /**
     * 车次不可购票的原因；返回 null 表示当前可购买。
     * 规则：在售 → 未发车 → 预售期内 → 未到停售时间。
     */
    private String sellTip(TrainBO bo, LocalDateTime now) {
        if (!bo.onSale()) {
            return "该车次暂不可售";
        }
        String windowTip = bo.saleWindowTip(now);
        if (windowTip != null) {
            return windowTip;
        }
        if (bo.getDepartDate() == null || bo.departed(now)) {
            return "已发车";
        }
        if (bo.getDepartDate().isAfter(now.toLocalDate().plusDays(presaleDays))) {
            return "预售期尚未开始";
        }
        if (bo.minutesToDepart(now) < stopSellMinutes) {
            return "开车前 " + stopSellMinutes + " 分钟停止售票";
        }
        return null;
    }

    public static String seatTypeName(Integer seatType) {
        if (seatType == null) {
            return "未知";
        }
        switch (seatType) {
            case Constants.SEAT_TYPE_BUSINESS:
                return "商务座";
            case Constants.SEAT_TYPE_FIRST:
                return "一等座";
            case Constants.SEAT_TYPE_SECOND:
                return "二等座";
            default:
                return "未知";
        }
    }
}

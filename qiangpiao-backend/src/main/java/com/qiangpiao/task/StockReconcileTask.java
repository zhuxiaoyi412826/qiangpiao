package com.qiangpiao.task;

import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.mapper.SeatMapper;
import com.qiangpiao.mapper.TrainMapper;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.SegmentStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务：秒杀库存三方对账（Redis 库存 / DB 余票 / 物理可售座位）。
 *
 * <pre>
 *   三个数正常情况下应该相等：
 *     redis = qp:seckill:stock:{trainId}:{seatType}   抢票入口的预扣计数
 *     db    = t_train_stock.available_count            落库扣减的余票
 *     seat  = t_seat 里 status=0 的座位数               物理事实，最可信
 *
 *   唯一的「合法不等」是抢票在途：Redis 已扣、订单还没落库，此时 redis &lt; db，
 *   且 qp:seckill:queue:pending 上有在途票数。除它以外都算漂移：
 *     · redis &gt; db      多退了 / 有人手动改过 → 有超卖风险，按 DB 收缩 Redis
 *     · redis &lt; db 且无在途  进程崩过 / 补偿漏了 → 按 DB 放行 Redis
 *     · db ≠ seat       扣库存与锁座位不在同一事务成功 → 会有「卖了没座」或「有座卖不掉」，
 *                       以物理座位数为准修 DB（区间票模式下座位可复用，只告警不自动修）
 * </pre>
 *
 * 防误修：同一处漂移要连续命中 {@code stock.reconcile-drift-threshold} 次才动手，
 * 否则可能把「刚扣 Redis、还没落库」的正常在途差当成漂移，把票多放出去造成超卖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockReconcileTask {

    /** 校准后 Redis 库存的 TTL，与 SeckillServiceImpl#preheatStock 保持一致 */
    private static final long STOCK_TTL_HOURS = 12;
    /** 漂移观察计数的有效期：期间不再漂移就自动作废 */
    private static final long DRIFT_TTL_MINUTES = 30;

    private final TrainMapper trainMapper;
    private final TrainStockMapper trainStockMapper;
    private final SeatMapper seatMapper;
    private final SegmentStockService segmentStockService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${stock.reconcile-enabled:true}")
    private boolean enabled;

    /** false = 只告警不改数据（灰度观察期用） */
    @Value("${stock.reconcile-fix-enabled:true}")
    private boolean fixEnabled;

    /**
     * 区间票模式下是否仍按「物理座位数」校准 DB 余票。
     * 区间票的座位可复用（同一座位卖 A-B 后还能卖 B-C），此时 t_seat 里 status=0 的座位数
     * 天然会少于 DB 余票，属于设计内差异——所以默认 false：只告警，不自动改，避免把正常复用当成漂移修掉。
     * 确认你的数据里没有区间复用（或愿意以座位为准）时再打开。
     */
    @Value("${stock.reconcile-seat-fix-enabled:false}")
    private boolean seatFixEnabled;

    @Value("${stock.reconcile-drift-threshold:2}")
    private int driftThreshold;

    @Value("${stock.reconcile-page-size:100}")
    private int pageSize;

    /**
     * 每 10 分钟对账一轮；首次延迟 3 分钟，等 StockPreheatTask 先预热完，避免开局满屏「未预热」。
     * 周期走配置，线上想跑得更勤（或临时关掉）直接改 config.properties，不用改代码。
     */
    @Scheduled(initialDelayString = "${stock.reconcile-initial-delay-ms:180000}",
            fixedDelayString = "${stock.reconcile-interval-ms:600000}")
    public void reconcile() {
        if (!enabled) {
            return;
        }
        long start = System.currentTimeMillis();
        int trains = 0;
        int stockRows = 0;
        int drift = 0;
        int fixed = 0;
        try {
            boolean segmentMode = segmentStockService.enabled();
            long total = trainMapper.countNotDeparted();
            int size = pageSize <= 0 ? 100 : pageSize;
            for (long offset = 0; offset < total; offset += size) {
                List<TrainDO> trainsPage = trainMapper.selectNotDeparted(offset, (long) size);
                if (trainsPage == null || trainsPage.isEmpty()) {
                    break;
                }
                for (TrainDO train : trainsPage) {
                    trains++;
                    Result r = reconcileTrain(train.getId(), segmentMode);
                    stockRows += r.scanned;
                    drift += r.drift;
                    fixed += r.fixed;
                }
            }
        } catch (Exception e) {
            log.error("[库存对账] 任务执行异常（本轮放弃，下轮重试）", e);
        }
        log.info("[库存对账] 完成：车次={}, 席别={}, 漂移={}, 已校准={}, 耗时={}ms",
                trains, stockRows, drift, fixed, System.currentTimeMillis() - start);
    }

    /**
     * 对账单个车次的所有席别。
     *
     * @param segmentMode 区间票模式：座位可区间复用，db 与 seat 的差异可能是设计内的，只告警不自动改
     */
    private Result reconcileTrain(Long trainId, boolean segmentMode) {
        List<TrainStockDO> stocks = trainStockMapper.selectByTrainId(trainId);
        if (stocks == null || stocks.isEmpty()) {
            return new Result();
        }
        int drift = 0;
        int fixed = 0;
        for (TrainStockDO stock : stocks) {
            Integer seatType = stock.getSeatType();
            if (seatType == null) {
                continue;
            }
            int db = stock.getAvailableCount() == null ? 0 : stock.getAvailableCount();
            int seat = seatMapper.countAvailable(trainId, seatType);

            /* ① DB 余票 vs 物理可售座位：以座位为准（卖票必须有座，座位是硬事实） */
            if (db != seat) {
                drift++;
                boolean autoFix = fixEnabled && (!segmentMode || seatFixEnabled);
                if (!autoFix) {
                    log.error("[库存对账] 座位与库存不一致（{}）：trainId={}, seatType={}, db={}, seat={}, diff={}",
                            segmentMode ? "区间票模式：座位可区间复用，默认只告警，确认非复用可开 stock.reconcile-seat-fix-enabled"
                                    : "已关闭自动校准，见 stock.reconcile-fix-enabled",
                            trainId, seatType, db, seat, db - seat);
                } else {
                    log.error("[库存对账] 座位与库存不一致：trainId={}, seatType={}, db={}, seat={}, diff={}",
                            trainId, seatType, db, seat, db - seat);
                    if (confirmDrift(trainId, seatType, "db")) {
                        trainStockMapper.updateAvailableCount(trainId, seatType, seat);
                        db = seat;
                        fixed++;
                        clearDrift(trainId, seatType, "db");
                        log.error("[库存对账] 已按物理座位数校准 DB 余票：trainId={}, seatType={}, {} -> {}",
                                trainId, seatType, stock.getAvailableCount(), seat);
                    }
                }
            }

            /* ② Redis 库存 vs DB 余票 */
            String key = RedisKeys.seckillStock(trainId, seatType);
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null) {
                // 没预热过，交给 StockPreheatTask，不算漂移
                continue;
            }
            int redis;
            try {
                redis = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                // 值被写坏了：直接按 DB 重置，不用等阈值
                log.error("[库存对账] Redis 库存值非法：key={}, value={}，已按 DB 重置", key, value);
                if (fixEnabled) {
                    setRedisStock(key, db);
                    fixed++;
                }
                continue;
            }
            if (redis == db) {
                clearDrift(trainId, seatType, "redis");
                continue;
            }
            int inflight = inflightCount(trainId, seatType);
            if (redis < db && inflight > 0) {
                // 正常在途：Redis 已扣、订单还没落库，不是漂移
                clearDrift(trainId, seatType, "redis");
                continue;
            }
            drift++;
            log.warn("[库存对账] Redis 与 DB 库存漂移：trainId={}, seatType={}, redis={}, db={}, diff={}, 在途={}",
                    trainId, seatType, redis, db, redis - db, inflight);
            if (fixEnabled && confirmDrift(trainId, seatType, "redis")) {
                setRedisStock(key, db);
                fixed++;
                clearDrift(trainId, seatType, "redis");
                log.warn("[库存对账] 已按 DB 余票校准 Redis 库存：trainId={}, seatType={}, {} -> {}",
                        trainId, seatType, redis, db);
            }
        }
        return new Result(stocks.size(), drift, fixed);
    }

    /** 在途票数：抢票已扣 Redis 但订单还没落库的数量（qp:seckill:queue:pending） */
    private int inflightCount(Long trainId, Integer seatType) {
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.seckillQueuePending(trainId, seatType));
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Math.max(Integer.parseInt(value.trim()), 0);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 漂移观察计数 +1，达到阈值才允许校准 */
    private boolean confirmDrift(Long trainId, Integer seatType, String dimension) {
        String key = RedisKeys.stockDrift(trainId, seatType, dimension);
        try {
            Long hits = stringRedisTemplate.opsForValue().increment(key);
            stringRedisTemplate.expire(key, DRIFT_TTL_MINUTES, TimeUnit.MINUTES);
            return hits != null && hits >= Math.max(driftThreshold, 1);
        } catch (Exception e) {
            // Redis 抖动时不擅自改数据，下轮再看
            log.warn("[库存对账] 漂移计数失败，本轮不校准：trainId={}, seatType={}, msg={}",
                    trainId, seatType, e.getMessage());
            return false;
        }
    }

    private void clearDrift(Long trainId, Integer seatType, String dimension) {
        try {
            stringRedisTemplate.delete(RedisKeys.stockDrift(trainId, seatType, dimension));
        } catch (Exception e) {
            log.debug("[库存对账] 清理漂移计数失败（无影响）：trainId={}, seatType={}", trainId, seatType);
        }
    }

    private void setRedisStock(String key, int value) {
        stringRedisTemplate.opsForValue().set(key, String.valueOf(value), STOCK_TTL_HOURS, TimeUnit.HOURS);
    }

    /** 单轮对账的小计，避免为统计单独建 VO */
    private static class Result {
        private final int scanned;
        private final int drift;
        private final int fixed;

        Result() {
            this(0, 0, 0);
        }

        Result(int scanned, int drift, int fixed) {
            this.scanned = scanned;
            this.drift = drift;
            this.fixed = fixed;
        }
    }
}

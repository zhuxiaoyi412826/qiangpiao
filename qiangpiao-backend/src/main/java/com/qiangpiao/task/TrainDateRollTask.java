package com.qiangpiao.task;

import com.qiangpiao.service.TrainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 车次日期滚动任务：保证「每天都有车次」。
 * <pre>
 *   车次的发车日期是固定日期，跨天之后旧日期的车次查不到（余票/座位也不再复用）。
 *   本任务把「发车日期早于今天」的车次统一滚到今天：
 *     - 应用启动 5 秒后执行一次（修复停机期间过期的日期）
 *     - 之后每小时检查一次（跨天后最多 1 小时内自动校正，无需重启）
 *   SQL 幂等（只更新 depart_date &lt; today 的行），开销极小。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainDateRollTask {

    private final TrainService trainService;

    @Scheduled(initialDelay = 5_000, fixedDelay = 3_600_000)
    public void roll() {
        try {
            trainService.rollExpiredTrains();
        } catch (Exception e) {
            log.error("车次日期滚动任务执行异常", e);
        }
    }
}

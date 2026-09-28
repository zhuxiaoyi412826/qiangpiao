package com.qiangpiao.task;

import com.qiangpiao.service.TrainScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日期班次生成任务：保证「每天都有车次可查询、可发车、可售卖」。
 * <pre>
 *   - 应用启动 10 秒后补齐一次（修复停机期间缺失的日期）
 *   - 之后每小时检查一次（跨天自动补出新一天，无需重启）
 *   生成范围 = ticket.query-days（默认 30 天），购买仍受预售期 14 天限制。
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrainScheduleTask {

    private final TrainScheduleService trainScheduleService;

    @Scheduled(initialDelay = 10_000, fixedDelay = 3_600_000)
    public void generate() {
        try {
            trainScheduleService.generateFutureTrains();
        } catch (Exception e) {
            log.error("日期班次生成任务执行异常", e);
        }
    }
}

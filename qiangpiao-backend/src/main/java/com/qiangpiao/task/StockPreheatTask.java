package com.qiangpiao.task;

import com.qiangpiao.service.SeckillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 定时任务：预热 / 校准 Redis 秒杀库存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPreheatTask {

    private final SeckillService seckillService;

    /**
     * 启动时预热一次（Redis 不可用时不影响启动）
     */
    @PostConstruct
    public void init() {
        try {
            seckillService.preheatAllStock();
        } catch (Exception e) {
            log.warn("启动预热秒杀库存失败（忽略，后续定时任务会重试）：{}", e.getMessage());
        }
    }

    /**
     * 每 10 分钟校准一次 Redis 库存
     */
    @Scheduled(fixedDelay = 600_000, initialDelay = 120_000)
    public void preheat() {
        try {
            seckillService.preheatAllStock();
        } catch (Exception e) {
            log.error("预热秒杀库存任务执行异常", e);
        }
    }
}

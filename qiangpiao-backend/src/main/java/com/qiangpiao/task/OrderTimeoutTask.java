package com.qiangpiao.task;

import com.qiangpiao.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务：关闭超时未支付订单（释放座位 + 归还库存）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    /**
     * 每分钟执行一次
     */
    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    public void closeExpiredOrders() {
        try {
            orderService.closeExpiredOrders();
        } catch (Exception e) {
            log.error("超时关单任务执行异常", e);
        }
    }
}

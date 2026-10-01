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
     * 每 30 秒执行一次：订单支付时限只有 5 分钟（order.pay-timeout-minutes），
     * 扫描间隔太长会让"已超时"的订单还挂在待支付里。
     */
    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    public void closeExpiredOrders() {
        try {
            orderService.closeExpiredOrders();
        } catch (Exception e) {
            log.error("超时关单任务执行异常", e);
        }
    }
}

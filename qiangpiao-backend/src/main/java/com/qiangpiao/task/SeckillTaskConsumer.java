package com.qiangpiao.task;

import com.qiangpiao.common.exception.SeckillTaskRetryException;
import com.qiangpiao.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 秒杀下单任务消费者：常驻线程读取 Redis Stream，落库成功 / 补偿完成才 XACK。
 * <pre>
 *   1) 启动即 ensureGroup，并先巡检一次 pending：把上次进程遗留的任务捞回来
 *   2) 阻塞读新消息（批量），交给 seckillExecutor 并行落库
 *   3) 成功 → XACK；业务失败（BizException）→ 已补偿 → XACK
 *      系统异常 → 不 XACK，等 pending 巡检重投；超过最大投递次数则补偿后 XACK
 * </pre>
 */
@Slf4j
@Component
public class SeckillTaskConsumer {

    private final SeckillTaskQueue queue;
    private final SeckillService seckillService;
    private final Executor executor;

    @Value("${seckill.task-claim-interval-ms:15000}")
    private long claimIntervalMs;
    @Value("${seckill.task-max-deliveries:3}")
    private long maxDeliveries;

    /** 消费者名带进程标识：多实例部署时各自领取，互不抢断 */
    private final String consumerName = "c-"
            + ManagementFactory.getRuntimeMXBean().getName().replace('@', '-');

    private volatile boolean running = false;
    private Thread worker;

    public SeckillTaskConsumer(SeckillTaskQueue queue, SeckillService seckillService,
                               @Qualifier("seckillExecutor") Executor executor) {
        this.queue = queue;
        this.seckillService = seckillService;
        this.executor = executor;
    }

    @PostConstruct
    public void start() {
        running = true;
        worker = new Thread(this::loop, "seckill-task-consumer");
        worker.setDaemon(true);
        worker.start();
        log.info("秒杀下单任务消费者已启动：consumer={}", consumerName);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        log.info("秒杀下单任务消费者已停止：consumer={}", consumerName);
    }

    private void loop() {
        queue.ensureGroup();
        // 启动先捞一次：上次进程崩溃未确认的任务，重启后立刻补做
        recoverPending();
        long lastClaim = System.currentTimeMillis();
        long lastTrim = System.currentTimeMillis();
        while (running) {
            try {
                long now = System.currentTimeMillis();
                if (now - lastClaim >= claimIntervalMs) {
                    lastClaim = now;
                    recoverPending();
                }
                if (now - lastTrim >= 600_000L) {
                    lastTrim = now;
                    queue.trim();
                }
                List<SeckillTaskQueue.TaskMessage> messages = queue.readNew(consumerName);
                if (!messages.isEmpty()) {
                    process(messages);
                }
            } catch (Exception e) {
                log.error("秒杀任务消费循环异常（1s 后继续）：msg={}", e.getMessage());
                sleepQuietly(1000);
            }
        }
    }

    /** pending 巡检：超时未确认的任务重投，超过投递上限的任务补偿后确认 */
    private void recoverPending() {
        try {
            List<SeckillTaskQueue.TaskMessage> pending = queue.claimIdle(consumerName);
            for (SeckillTaskQueue.TaskMessage message : pending) {
                if (message.getTask() == null) {
                    queue.ack(message.getId());
                    continue;
                }
                if (message.getDeliveries() >= maxDeliveries) {
                    log.error("任务投递次数超限，补偿回滚：id={}, orderNo={}, deliveries={}",
                            message.getId(), message.getTask().getOrderNo(), message.getDeliveries());
                    seckillService.abandon(message.getTask(), "下单失败（任务重试超限），库存已回滚");
                    queue.ack(message.getId());
                } else {
                    process(Collections.singletonList(message));
                }
            }
        } catch (Exception e) {
            log.error("巡检 pending 异常（下个周期重试）：msg={}", e.getMessage());
        }
    }

    /** 并行处理一批任务，全部结束后再统一确认 */
    private void process(List<SeckillTaskQueue.TaskMessage> messages) {
        List<CompletableFuture<Boolean>> futures = new ArrayList<>(messages.size());
        List<String> ids = new ArrayList<>(messages.size());
        for (SeckillTaskQueue.TaskMessage message : messages) {
            ids.add(message.getId());
            futures.add(CompletableFuture.supplyAsync(() -> handle(message), executor));
        }
        for (int i = 0; i < futures.size(); i++) {
            boolean ack;
            try {
                ack = futures.get(i).get();
            } catch (Exception e) {
                log.error("等待任务结果异常（本次不确认，等待重投）：id={}, msg={}", ids.get(i), e.getMessage());
                ack = false;
            }
            if (ack) {
                safeAck(ids.get(i));
            }
        }
    }

    /** @return true = 可以确认（成功或已终结补偿）；false = 保留 pending 等待重投 */
    private boolean handle(SeckillTaskQueue.TaskMessage message) {
        if (message.getTask() == null) {
            return true;
        }
        try {
            seckillService.processTask(message.getTask());
            return true;
        } catch (SeckillTaskRetryException e) {
            log.warn("下单任务暂不确认，等待 pending 重投：orderNo={}, msg={}",
                    message.getTask().getOrderNo(), e.getMessage());
            return false;
        } catch (Throwable t) {
            log.error("下单任务处理异常：orderNo={}, msg={}",
                    message.getTask().getOrderNo(), t.getMessage());
            if (message.getDeliveries() >= maxDeliveries) {
                seckillService.abandon(message.getTask(), "下单失败（任务重试超限），库存已回滚");
                return true;
            }
            return false;
        }
    }

    private void safeAck(String id) {
        try {
            queue.ack(id);
        } catch (Exception e) {
            log.error("任务确认失败（会重复投递一次，业务层有幂等兜底）：id={}, msg={}", id, e.getMessage());
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

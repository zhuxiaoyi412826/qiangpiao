package com.qiangpiao.service;

import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.dto.SeckillDTO;
import com.qiangpiao.vo.SeckillBatchResultVO;
import com.qiangpiao.vo.SeckillResultVO;
import com.qiangpiao.vo.SeckillStatusVO;

/**
 * 秒杀抢票服务：Redis 预扣库存 + 异步落库 + 一人一单 + 限流。
 */
public interface SeckillService {

    /**
     * 抢票：同步完成库存预扣，异步创建订单
     */
    SeckillResultVO seckill(Long userId, SeckillDTO seckillDTO, String ip);

    /**
     * 轮询抢票结果（单张）
     */
    SeckillStatusVO queryResult(Long trainId, Integer seatType, Long userId);

    /**
     * 轮询批量抢票结果（一次买多张）
     *
     * @param batchNo 下单返回的批次号
     */
    SeckillBatchResultVO queryBatchResult(Long userId, String batchNo);

    /**
     * 预热单个车次的秒杀库存到 Redis
     */
    void preheatStock(Long trainId);

    /**
     * 预热全部车次库存（系统启动 / 定时任务）
     */
    void preheatAllStock();

    /**
     * 查询 Redis 中的实时余票（-1 表示未预热）
     */
    int availableStock(Long trainId, Integer seatType);

    /**
     * 回滚 Redis 库存（下单失败 / 取消订单补偿）
     */
    void rollbackStock(Long trainId, Integer seatType);

    /**
     * 处理一条下单任务（Redis Stream 消费者调用）。
     * <p>
     * 业务上注定失败的情况（限购 / 库存不足 / 重复下单）内部已补偿回滚，正常返回；
     * 系统抖动（DB 超时等）抛 SeckillTaskRetryException，由队列保留 pending 等待重投。
     */
    void processTask(SeckillTaskBO taskBO);

    /**
     * 终结一条任务：补偿回滚 Redis 库存与购票额度，并把该票标记为失败。
     * 用于任务重试次数耗尽时（消费者侧调用），保证「扣了库存却没成单」的状态一定被回收。
     */
    void abandon(SeckillTaskBO taskBO, String reason);
}

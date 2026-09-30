package com.qiangpiao.service;

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
}

package com.qiangpiao.service;

import com.qiangpiao.bo.RangeBO;

import java.util.List;

/**
 * 区间票库存服务：把「上车站 → 下车站」翻译成经停站序号区间，并按「相邻站单段」管理余票。
 *
 * <pre>
 *   座位复用：A-B-C 三站，座位 S 卖掉 A-B 后，B-C 仍可再卖一次（区间不重叠即可共用同一座位）
 *   余票计算：OD 区间余票 = 该区间覆盖的每一段余票的最小值（木桶效应）
 *   防超卖  ：Redis 多段 Lua 原子扣减（一段不够就整批失败），DB 段库存条件更新作最后一道防线
 * </pre>
 * 未执行建表脚本时 {@link #enabled()} 返回 false，全部逻辑退化为原有全程票，不影响现有功能。
 */
public interface SegmentStockService {

    /** 区间票能力是否可用（t_train_segment_stock / t_seat_segment 已建表） */
    boolean enabled();

    /**
     * 解析乘车区间：站名 → 经停站序号。站名为空或匹配不到时退化为全程票。
     */
    RangeBO resolveRange(Long trainId, String fromStation, String toStation);

    /**
     * 区间余票：Redis（已预热）优先，否则查 DB 段库存。
     *
     * @return 余票数；区间能力未开启时返回 null（调用方按原有全程库存处理）
     */
    Integer available(Long trainId, Integer seatType, RangeBO range);

    /** 区间覆盖的 Redis 库存 key（秒杀多段扣减用） */
    List<String> stockKeys(Long trainId, Integer seatType, RangeBO range);

    /**
     * Redis 多段原子扣减（秒杀预扣）：覆盖的每一段都要够票，一段不足则整批失败且不扣任何段。
     *
     * @return 1 成功 / -1 有段未初始化 / -2 有段余票不足
     */
    int deductRedis(Long trainId, Integer seatType, RangeBO range, int count);

    /** Redis 多段回滚（下单失败 / 退票 / 取消时归还） */
    void rollbackRedis(Long trainId, Integer seatType, RangeBO range, int count);

    /**
     * 占用区间库存（DB，需在事务内调用）。
     *
     * @return true 全部段扣减成功
     */
    boolean occupy(Long trainId, Integer seatType, RangeBO range, int count);

    /** 释放区间库存（DB，退票 / 取消 / 补偿回滚） */
    void release(Long trainId, Integer seatType, RangeBO range, int count);

    /**
     * 初始化车次的区间库存与 Redis 预热：为每个席别的每一段写入 total / available。
     *
     * @return 初始化的段数量
     */
    int initSegments(Long trainId);

    /** 车次的经停站数量（用于判断是否存在分段数据） */
    int stopCount(Long trainId);
}

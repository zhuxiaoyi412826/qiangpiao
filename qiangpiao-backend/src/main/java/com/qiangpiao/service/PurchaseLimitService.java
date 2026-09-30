package com.qiangpiao.service;

import com.qiangpiao.bo.TrainBO;

import java.util.List;

/**
 * 购票限购规则服务。
 * <pre>
 *   规则一：同一车次最多购买 MAX_TICKETS(9) 张（不分席别，可多乘客）
 *   规则二：已购车次处于运行时间内（发车时刻 ~ 到达时刻）不能再买其他车次，
 *           必须等该车次到达（下车）后才可继续购票（同一车次多张票不冲突）
 *   规则三：同一乘车人（身份证）在同一车次只能有一张有效票；同一批次内也不能重复
 * </pre>
 */
public interface PurchaseLimitService {

    /** 同一车次最多可购买票数（可不同乘车人） */
    int MAX_TICKETS_PER_TRAIN = 9;

    /**
     * 购票前校验（单张），不满足规则时抛出业务异常（带明确文案）。
     *
     * @param userId 购票用户
     * @param train  目标车次
     */
    void assertCanBuy(Long userId, TrainBO train);

    /**
     * 批量购票（一次多张）前校验。
     *
     * @param userId     购票用户
     * @param train      目标车次
     * @param count      本次购买张数
     * @param idCards    每位乘车人的身份证密文（用于同车次去重 / 批次内去重）
     */
    void assertCanBuyBatch(Long userId, TrainBO train, int count, List<String> idCards);

    /**
     * 是否还能购买该车次（不抛异常，供前端展示"已购 / 行程冲突"标记）。
     *
     * @return null 表示可以购买，否则为不可购买的原因
     */
    String buyBlockReason(Long userId, TrainBO train);
}

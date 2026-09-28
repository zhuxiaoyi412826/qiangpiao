package com.qiangpiao.service;

import com.qiangpiao.bo.TrainBO;

/**
 * 购票限购规则服务。
 * <pre>
 *   规则一：每人每天每车次限购 1 张（不分席别）
 *   规则二：已购车次处于运行时间内（发车时刻 ~ 到达时刻）不能再买其他车次，
 *           必须等该车次到达（下车）后才可继续购票
 * </pre>
 */
public interface PurchaseLimitService {

    /**
     * 购票前校验，不满足规则时抛出业务异常（带明确文案）。
     *
     * @param userId 购票用户
     * @param train  目标车次
     */
    void assertCanBuy(Long userId, TrainBO train);

    /**
     * 是否还能购买该车次（不抛异常，供前端展示"已购 / 行程冲突"标记）。
     *
     * @return null 表示可以购买，否则为不可购买的原因
     */
    String buyBlockReason(Long userId, TrainBO train);
}

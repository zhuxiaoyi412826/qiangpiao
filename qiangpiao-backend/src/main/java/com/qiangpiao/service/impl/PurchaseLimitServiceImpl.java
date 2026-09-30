package com.qiangpiao.service.impl;

import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.service.PurchaseLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 购票限购规则实现：
 * <pre>
 *   1) 同一车次最多 9 张：t_order 按 (train_id, user_id) 统计有效订单（待支付 / 已支付）。
 *      train_id 本身就是「某一天的实际班次」（uk_train_no_date），所以天然满足"每天每车次"；
 *      一次买多张时按「已购张数 + 本次张数」判断，上限 9。
 *   2) 行程运行时间冲突：用户已购有效订单的运行区间 [发车, 到达] 与目标车次区间重叠则禁止购买，
 *      必须等已购车次到达（下车）后才行；同一车次多张票不算冲突（排除 train_id）。
 *   3) 同一乘车人（身份证密文）在同一车次只能有一张有效票，且同一批次内不可重复。
 * </pre>
 * 有效订单 = 待支付(0) / 已支付(1)；已取消、已退票、已超时的订单不再占用名额。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseLimitServiceImpl implements PurchaseLimitService {

    private final OrderMapper orderMapper;

    @Override
    public void assertCanBuy(Long userId, TrainBO train) {
        String reason = buyBlockReason(userId, train);
        if (reason != null) {
            log.info("购票被限购规则拦截：userId={}, trainId={}, trainNo={}, reason={}",
                    userId, train == null ? null : train.getId(), train == null ? null : train.getTrainNo(), reason);
            throw new BizException(ResultCode.BUY_LIMIT_PER_TRAIN, reason);
        }
    }

    @Override
    public void assertCanBuyBatch(Long userId, TrainBO train, int count, List<String> idCards) {
        if (count <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (count > MAX_TICKETS_PER_TRAIN) {
            throw new BizException(ResultCode.SECKILL_BATCH_LIMIT);
        }
        if (userId == null || train == null || train.getId() == null) {
            return;
        }

        // 规则三（批次内）：同一乘车人不能在同一批里买两张
        if (idCards != null) {
            Set<String> unique = new HashSet<>();
            for (String card : idCards) {
                if (!StringUtils.hasText(card) || !unique.add(card)) {
                    throw new BizException(ResultCode.SECKILL_BATCH_DUPLICATE);
                }
            }
            // 规则三（库内）：该乘车人已买过该车次
            for (String card : unique) {
                if (orderMapper.countByTrainAndIdCard(train.getId(), card) > 0) {
                    throw new BizException(ResultCode.PASSENGER_TICKET_EXISTS,
                            "乘车人已购买 " + train.getTrainNo() + "（" + train.getDepartDate()
                                    + " " + train.getDepartTime() + " 发车）的车票，不可重复购买");
                }
            }
        }

        // 规则一：已购张数 + 本次张数 <= 9
        int bought = orderMapper.countByTrainAndUser(train.getId(), userId);
        if (bought + count > MAX_TICKETS_PER_TRAIN) {
            throw new BizException(ResultCode.BUY_LIMIT_PER_TRAIN,
                    "每车次最多购买 " + MAX_TICKETS_PER_TRAIN + " 张票，您已购买 " + bought + " 张，本次还可购买 "
                            + Math.max(MAX_TICKETS_PER_TRAIN - bought, 0) + " 张");
        }

        // 规则二：行程运行时间冲突（同车次不算）
        LocalDateTime start = train.departAt();
        LocalDateTime end = train.arriveAt();
        if (start == null || end == null) {
            return;
        }
        List<Map<String, Object>> conflicts = orderMapper.selectTripConflicts(userId, start, end, train.getId());
        if (conflicts == null || conflicts.isEmpty()) {
            return;
        }
        Map<String, Object> conflict = conflicts.get(0);
        throw new BizException(ResultCode.TRIP_TIME_CONFLICT,
                "您已购买 " + conflict.get("trainNo") + " "
                        + conflict.get("fromStationName") + "→" + conflict.get("toStationName")
                        + "（" + conflict.get("departDate") + " " + conflict.get("departTime") + " 发车，"
                        + conflict.get("arriveTime") + " 到达），该车次到达（下车）后才能再次购票");
    }

    @Override
    public String buyBlockReason(Long userId, TrainBO train) {
        if (userId == null || train == null || train.getId() == null) {
            return null;
        }

        // 规则一（预检展示）：已买满 9 张则提示
        int bought = orderMapper.countByTrainAndUser(train.getId(), userId);
        if (bought >= MAX_TICKETS_PER_TRAIN) {
            return "每车次最多购买 " + MAX_TICKETS_PER_TRAIN + " 张票，您已购买 " + bought + " 张";
        }

        // 规则二：行程运行时间冲突（车还在跑，人还在车上，不能再买）
        LocalDateTime start = train.departAt();
        LocalDateTime end = train.arriveAt();
        if (start == null || end == null) {
            return null;
        }
        List<Map<String, Object>> conflicts = orderMapper.selectTripConflicts(userId, start, end, train.getId());
        if (conflicts == null || conflicts.isEmpty()) {
            return null;
        }
        Map<String, Object> conflict = conflicts.get(0);
        return "您已购买 " + conflict.get("trainNo") + " "
                + conflict.get("fromStationName") + "→" + conflict.get("toStationName")
                + "（" + conflict.get("departDate") + " " + conflict.get("departTime") + " 发车，"
                + conflict.get("arriveTime") + " 到达），该车次到达（下车）后才能再次购票";
    }
}

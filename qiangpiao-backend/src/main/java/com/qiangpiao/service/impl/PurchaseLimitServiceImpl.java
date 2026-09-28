package com.qiangpiao.service.impl;

import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.service.PurchaseLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 购票限购规则实现：
 * <pre>
 *   1) 每人每天每车次限购 1 张：t_order 按 (train_id, user_id) 统计有效订单（待支付 / 已支付）
 *      train_id 本身就是「某一天的实际班次」（uk_train_no_date），所以天然满足"每天每车次"
 *   2) 行程运行时间冲突：用户已购有效订单的运行区间 [发车, 到达] 与目标车次区间重叠则禁止购买，
 *      必须等已购车次到达（下车）后才行
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
    public String buyBlockReason(Long userId, TrainBO train) {
        if (userId == null || train == null || train.getId() == null) {
            return null;
        }

        // 规则一：每人每天每车次限购 1 张
        if (orderMapper.countByTrainAndUser(train.getId(), userId) > 0) {
            return "每人每天每车次限购 1 张，您已购买 " + train.getTrainNo()
                    + "（" + train.getDepartDate() + " " + train.getDepartTime() + " 发车）的车票";
        }

        // 规则二：行程运行时间冲突（车还在跑，人还在车上，不能再买）
        LocalDateTime start = train.departAt();
        LocalDateTime end = train.arriveAt();
        if (start == null || end == null) {
            return null;
        }
        List<Map<String, Object>> conflicts = orderMapper.selectTripConflicts(userId, start, end);
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

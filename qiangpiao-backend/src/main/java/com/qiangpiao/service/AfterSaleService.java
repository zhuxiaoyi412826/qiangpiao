package com.qiangpiao.service;

import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.vo.AfterSalePreviewVO;

import java.util.List;

/**
 * 售后服务：退票 / 改签 / 售后记录（时间轴与改签历史）。
 * 用户自助与后台客服人工处理复用同一套逻辑，区别在于 operator 与是否校验归属。
 *
 * <pre>
 *   退票手续费：按距开车时间阶梯计费（8 天以上免费 / 5% / 10% / 20%），
 *              尾数 5 角取整、最低 2 元；发车后当日 24 点前 50%，次日及以后不再办理退票；
 *              改签过的票按「最初购票车次」的开车时间取档，春运期间不低于 20%，开车后不可退。
 *   改签手续费：一张车票只能改签 1 次；按「新旧两张票里较低票价」为基数，
 *              场景费率见 ChangeFeeUtil（0% / 5% / 15% / 40%），与差额合并为净额多退少补。
 * </pre>
 */
public interface AfterSaleService {

    /**
     * 退票：扣手续费 → 实退金额退回钱包 → 释放座位与库存 → 写流转日志。
     * 幂等：同一订单重复提交只会成功一次（CAS 状态 + 钱包幂等键 + Redis 锁）。
     *
     * @param userId   前台自助退票时传用户ID做归属校验；后台人工退票可传 null 跳过归属校验
     * @param operator 操作人标记（用户ID 或 admin）
     */
    void refund(String orderNo, Long userId, String operator, String reason);

    /**
     * 改签：换到新车次同席别，差额多退少补（退差额按阶梯扣手续费），写改签记录与流转日志。
     */
    void change(String orderNo, Long userId, Long newTrainId, Integer seatType, String reason);

    /** 退票费用试算（不落库）：票价 / 手续费 / 实退金额 / 计费档位 */
    AfterSalePreviewVO previewRefund(String orderNo, Long userId);

    /** 改签费用试算（不落库）：差额 / 手续费 / 实退或实补 */
    AfterSalePreviewVO previewChange(String orderNo, Long userId, Long newTrainId, Integer seatType);

    /** 订单时间轴（物流式节点） */
    List<OrderLogDO> timeline(String orderNo);

    /** 改签历史 */
    List<OrderChangeDO> changes(String orderNo);
}

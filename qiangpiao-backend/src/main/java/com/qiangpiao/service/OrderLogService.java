package com.qiangpiao.service;

import com.qiangpiao.common.constant.OrderAction;
import com.qiangpiao.dataobject.OrderLogDO;

import java.util.List;

/**
 * 订单流转日志服务：全链路业务日志的统一入口。
 * <p>
 * 业务动作（下单 / 支付 / 取消 / 超时 / 退票 / 改签 / 抢票受理与失败）都通过它写 t_order_log，
 * 保证时间轴节点齐全、action 取值受枚举约束、traceId 贯穿始终。
 */
public interface OrderLogService {

    /**
     * 同步写日志（与业务同事务：业务回滚则日志一起回滚）。
     * 日志写入失败只告警，绝不中断主流程。
     */
    void log(String orderNo, OrderAction action, String detail, String operator);

    /**
     * 异步写日志：秒杀受理 / 抢票失败这类高频节点用，避免额外的一次 DB 写入拖慢抢票响应。
     */
    void logAsync(String orderNo, OrderAction action, String detail, String operator);

    /** 单个订单的流转时间轴 */
    List<OrderLogDO> timeline(String orderNo);

    /** 批量订单的流转日志 */
    List<OrderLogDO> timelines(List<String> orderNos);
}

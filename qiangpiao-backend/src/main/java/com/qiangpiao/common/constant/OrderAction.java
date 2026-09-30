package com.qiangpiao.common.constant;

/**
 * 订单流转动作枚举（对应 t_order_log.action）。
 * <p>
 * 覆盖「抢票 → 下单 → 支付 → 完成 / 取消 / 超时 / 退票 / 改签」全链路，
 * 订单详情页的时间轴按 create_time 顺序渲染这些节点。
 */
public enum OrderAction {

    /** 抢票受理：秒杀请求已通过校验，进入异步下单队列（12306 的「排队中」） */
    SECKILL_ACCEPT("抢票受理"),
    /** 抢票失败：异步下单异常，库存与额度已回滚 */
    SECKILL_FAIL("抢票失败"),
    /** 下单成功：订单落库，待支付 */
    CREATE("下单成功"),
    /** 发起支付：支付单已创建，等待渠道回调 */
    PAY_CREATE("发起支付"),
    /** 支付成功 */
    PAY("支付成功"),
    /** 支付失败：渠道返回失败 */
    PAY_FAIL("支付失败"),
    /** 支付异常：回调成功但订单状态已变更，未扣款 */
    PAY_ABNORMAL("支付异常"),
    /** 支付单关闭：订单取消 / 超时后关闭未终态支付单 */
    PAY_CLOSE("支付单关闭"),
    /** 取消订单：用户主动取消 */
    CANCEL("取消订单"),
    /** 超时关单：支付超时被系统自动关闭 */
    EXPIRE("超时关单"),
    /** 退票成功 */
    REFUND("退票成功"),
    /** 改签成功 */
    CHANGE("改签成功");

    private final String text;

    OrderAction(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}

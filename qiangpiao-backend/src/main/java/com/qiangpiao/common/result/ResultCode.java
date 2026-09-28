package com.qiangpiao.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一响应状态码。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /* ========== 通用 ========== */
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方式不支持"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    /* ========== 用户 / 认证 1000+ ========== */
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "账号已被禁用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    PASSWORD_ERROR(1004, "用户名或密码错误"),
    PHONE_INVALID(1005, "手机号格式不正确"),

    /* ========== 车次 / 座位 2000+ ========== */
    TRAIN_NOT_FOUND(2001, "车次不存在"),
    TRAIN_NOT_SALE(2002, "该车次暂不可售"),
    SEAT_TYPE_INVALID(2003, "席别不合法"),
    SEAT_NOT_FOUND(2004, "座位不存在"),
    SEAT_SOLD(2005, "座位已被抢走，请重新选择"),
    STOCK_NOT_ENOUGH(2006, "余票不足"),
    STOCK_NOT_INIT(2007, "秒杀库存未初始化，请稍后再试"),
    TRAIN_QUERY_DATE_INVALID(2008, "只能查询今天起 30 天内的车次"),
    TRAIN_DEPARTED(2009, "该车次已发车，无法购票"),
    TICKET_STOP_SELL(2010, "开车前 20 分钟停止售票"),
    TICKET_NOT_ON_SALE(2011, "该车次尚未开始预售，预售期为 14 天"),

    /* ========== 秒杀 3000+ ========== */
    SECKILL_NOT_START(3001, "秒杀活动未开始"),
    SECKILL_FINISHED(3002, "秒杀活动已结束"),
    SECKILL_REPEAT(3003, "请勿重复抢票"),
    SECKILL_FAILED(3004, "抢票失败，请重试"),
    ORDER_QUEUEING(3005, "排队中，请稍后查询结果"),
    /** 限购：每人每天每车次 1 张 */
    BUY_LIMIT_PER_TRAIN(3006, "每人每天每车次限购 1 张"),
    /** 限购：已购车次的运行时间（发车~到达）内不能重复购票 */
    TRIP_TIME_CONFLICT(3007, "您已购买的车次仍在运行时间内，下车后才能再次购票"),

    /* ========== 订单 4000+ ========== */
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态不正确"),
    ORDER_EXPIRED(4003, "订单已超时，请重新下单"),
    ORDER_PAY_FAILED(4004, "支付失败"),

    /* ========== 钱包 5000+ ========== */
    WALLET_NOT_FOUND(5001, "钱包不存在，请稍后重试"),
    WALLET_BALANCE_NOT_ENOUGH(5002, "余额不足，请到个人中心 - 我的钱包充值"),
    WALLET_AMOUNT_INVALID(5003, "金额不合法"),
    WALLET_RECHARGE_LIMIT(5004, "单笔充值金额不能超过 50000 元"),
    WALLET_OPERATE_FAILED(5005, "钱包扣款失败，请稍后重试");

    private final int code;
    private final String message;
}

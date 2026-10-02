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
    // 文案刻意区别于限流的「系统繁忙」：500 只代表服务端异常，便于一眼区分是限流还是真报错
    SYSTEM_ERROR(500, "服务异常，请稍后重试"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    /* ========== 用户 / 认证 1000+ ========== */
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "账号已被禁用"),
    USERNAME_EXISTS(1003, "用户名已存在"),
    PASSWORD_ERROR(1004, "用户名或密码错误"),
    PHONE_INVALID(1005, "手机号格式不正确"),
    ID_CARD_INVALID(1006, "身份证号格式不正确"),

    /* ========== 常用乘车人 1100+ ========== */
    PASSENGER_NOT_FOUND(1101, "常用乘车人不存在"),
    /** 常用乘车人数量上限 */
    PASSENGER_LIMIT(1102, "常用乘车人最多 3 位，请先删除后再添加"),
    PASSENGER_ID_CARD_EXISTS(1103, "该身份证号已在您的常用乘车人中"),

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
    /** 同一车次号在同一日期已有班次（uk_train_date）：生成当日班次撞车 */
    TRAIN_DATE_EXISTS(2011, "该日期的班次已存在，无需重复生成"),
    TICKET_NOT_ON_SALE(2011, "该车次尚未开始预售，预售期为 14 天"),

    /* ========== 秒杀 3000+ ========== */
    SECKILL_NOT_START(3001, "秒杀活动未开始"),
    SECKILL_FINISHED(3002, "秒杀活动已结束"),
    SECKILL_REPEAT(3003, "请勿重复抢票"),
    SECKILL_FAILED(3004, "抢票失败，请重试"),
    ORDER_QUEUEING(3005, "排队中，请稍后查询结果"),
    /** 限购：单用户单车次最多 9 张（可多乘客） */
    BUY_LIMIT_PER_TRAIN(3006, "每车次最多购买 9 张票"),
    /** 一次下单张数超过上限 */
    SECKILL_BATCH_LIMIT(3008, "一次最多购买 9 张票"),
    /** 同一批次内同一乘车人重复 */
    SECKILL_BATCH_DUPLICATE(3009, "同一乘车人不可重复购票"),
    /** 该乘车人已购买该车次 */
    PASSENGER_TICKET_EXISTS(3010, "该乘车人已购买该车次车票"),
    /** 限购：已购车次的运行时间（发车~到达）内不能重复购票 */
    TRIP_TIME_CONFLICT(3007, "您已购买的车次仍在运行时间内，下车后才能再次购票"),

    /* ========== 订单 4000+ ========== */
    ORDER_NOT_FOUND(4001, "订单不存在"),
    ORDER_STATUS_ERROR(4002, "订单状态不正确"),
    ORDER_EXPIRED(4003, "订单已超时，请重新下单"),
    ORDER_PAY_FAILED(4004, "支付失败"),
    ORDER_PAYING(4005, "支付处理中，请勿重复发起"),
    PAYMENT_NOT_FOUND(4006, "支付单不存在"),
    PAY_SIGN_INVALID(4007, "回调验签失败"),
    PAY_AMOUNT_MISMATCH(4008, "回调金额与支付单不一致"),
    PAY_NOTIFY_EXPIRED(4009, "回调已过期：时间戳超出有效窗口"),
    /** 重复退票：并发锁未拿到 / 订单已退 */
    REFUND_REPEAT(4010, "该订单已退票或正在退票中，请勿重复提交"),
    /** 开车后未改签的车票：不可退 */
    REFUND_AFTER_DEPART(4011, "列车已发车，开车后不可退票"),
    /** 开车后改签过的车票：不可退 */
    REFUND_CHANGED_AFTER_DEPART(4012, "改签过的车票开车后不可退票"),
    /** 改签校验不通过 */
    CHANGE_NOT_ALLOWED(4013, "该订单当前不可改签"),
    /** 一张车票只能改签一次 */
    CHANGE_ONLY_ONCE(4014, "一张车票只能改签一次，该票已办理过改签"),
    /** 开车后超过当日 24 点再改签 / 退票 */
    CHANGE_EXPIRED(4015, "开车后仅可在乘车当日 24 点前改签，现已超出办理时间"),
    /** 发车次日及以后退票 */
    REFUND_EXPIRED(4016, "已超过退票时限：发车次日及以后不再办理退票"),
    /** 区间票暂不支持改签 */
    CHANGE_SEGMENT_UNSUPPORTED(4017, "区间票暂不支持改签，请先退票后重新购票"),
    /** 车次售卖时间窗口：未开始 / 已结束 */
    SALE_NOT_START(4018, "该车次尚未开始售票，请等到售票开始时间"),
    SALE_ENDED(4019, "该车次售票已结束"),
    /** 订单已支付：渠道回调先到、页面还是旧状态时重复点支付，提示刷新而非"状态不正确" */
    ORDER_ALREADY_PAID(4025, "订单已支付，无需重复支付"),

    /* ========== 风控 / 限流 4020+ ========== */
    /** IP 维度限流 */
    IP_TOO_MANY_REQUESTS(4020, "当前网络请求过于频繁，请稍后再试"),
    /** IP 黑名单 */
    IP_BLOCKED(4021, "该 IP 已被限制访问，如有疑问请联系客服"),
    /** 全局令牌桶限流 */
    SYSTEM_BUSY_GLOBAL(4022, "当前抢票人数过多，系统繁忙，请稍后再试"),
    /** 抢票前置人机验证未通过 */
    CAPTCHA_REQUIRED(4023, "请先完成人机验证再抢票"),
    CAPTCHA_INVALID(4024, "人机验证失败或已失效，请重新验证"),

    /* ========== 钱包 5000+ ========== */
    WALLET_NOT_FOUND(5001, "钱包不存在，请稍后重试"),
    WALLET_BALANCE_NOT_ENOUGH(5002, "余额不足，请到个人中心 - 我的钱包充值"),
    WALLET_AMOUNT_INVALID(5003, "金额不合法"),
    WALLET_RECHARGE_LIMIT(5004, "单笔充值金额不能超过 50000 元"),
    WALLET_OPERATE_FAILED(5005, "钱包扣款失败，请稍后重试"),

    /* ========== 资金对账 6000+ ========== */
    /** 对账单据不存在 */
    RECON_BILL_NOT_FOUND(6001, "对账单据不存在"),
    /** 只有「待审核」的单据能被审核，重复审核 / 审核已处理的单据会撞这个 */
    RECON_BILL_STATUS_ERROR(6002, "单据状态不正确，只有待审核的单据可以处理"),
    /** 审核通过但退补账执行失败（如用户余额不足，补扣失败） */
    RECON_HANDLE_FAILED(6003, "退补账执行失败，请查看日志或稍后重试");

    private final int code;
    private final String message;
}

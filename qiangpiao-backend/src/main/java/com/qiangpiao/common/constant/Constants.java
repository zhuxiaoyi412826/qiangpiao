package com.qiangpiao.common.constant;

/**
 * 全局常量。
 */
public final class Constants {

    private Constants() {
    }

    /** 请求头中携带 Token 的 key */
    public static final String TOKEN_HEADER = "Authorization";
    /** Token 前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /* ========== 缓存 / Redis key 前缀 ========== */
    public static final String CACHE_TRAIN_LIST = "qp:cache:train:list:";
    public static final String CACHE_TRAIN_DETAIL = "qp:cache:train:detail:";
    public static final String CACHE_STATION = "qp:cache:station:all";
    public static final String CACHE_SEAT_MAP = "qp:cache:seat:map:";
    public static final String CACHE_ORDER_DETAIL = "qp:cache:order:detail:";

    /** 秒杀库存 */
    public static final String STOCK_KEY = "qp:seckill:stock:";
    /** 一人一单标记 */
    public static final String SECKILL_USER_KEY = "qp:seckill:user:";
    /** 用户抢票结果（订单号） */
    public static final String SECKILL_RESULT_KEY = "qp:seckill:result:";
    /** 接口限流 */
    public static final String LIMIT_KEY = "qp:limit:user:";
    /** 支付回调 nonce 防重放 */
    public static final String PAY_NOTIFY_NONCE_KEY = "qp:pay:notify:nonce:";
    /** 分布式锁 */
    public static final String LOCK_KEY = "qp:lock:";
    /** 缓存空值占位，防穿透 */
    public static final String CACHE_NULL_VALUE = "__NULL__";

    /* ========== 状态 ========== */
    /** 座位状态：可售 */
    public static final int SEAT_STATUS_AVAILABLE = 0;
    /** 座位状态：已售 */
    public static final int SEAT_STATUS_SOLD = 1;
    /** 座位状态：锁定 */
    public static final int SEAT_STATUS_LOCKED = 2;

    /** 订单状态：待支付 */
    public static final int ORDER_STATUS_WAIT_PAY = 0;
    /** 订单状态：已支付 */
    public static final int ORDER_STATUS_PAID = 1;
    /** 订单状态：已取消 */
    public static final int ORDER_STATUS_CANCELLED = 2;
    /** 订单状态：已退票 */
    public static final int ORDER_STATUS_REFUNDED = 3;
    /** 订单状态：已超时 */
    public static final int ORDER_STATUS_EXPIRED = 4;
    /** 已改签：原订单被改签到新车次后保留的历史状态 */
    public static final int ORDER_STATUS_CHANGED = 5;

    /* ========== 支付单状态（t_payment.status） ========== */
    /** 支付中：已发起，等待渠道异步回调 */
    public static final int PAY_STATUS_PAYING = 0;
    /** 支付成功：已收到成功回调并完成订单入账 */
    public static final int PAY_STATUS_SUCCESS = 1;
    /** 支付失败：渠道返回失败或入账失败 */
    public static final int PAY_STATUS_FAILED = 2;
    /** 已关闭：订单取消 / 超时，支付单不再接受回调 */
    public static final int PAY_STATUS_CLOSED = 3;

    /** 用户状态：正常 */
    public static final int USER_STATUS_NORMAL = 1;
    /** 用户状态：禁用 */
    public static final int USER_STATUS_DISABLED = 0;

    /* ========== 席别 ========== */
    public static final int SEAT_TYPE_BUSINESS = 1;
    public static final int SEAT_TYPE_FIRST = 2;
    public static final int SEAT_TYPE_SECOND = 3;
    public static final int SEAT_TYPE_SOFT_SLEEPER = 4;
    public static final int SEAT_TYPE_HARD_SLEEPER = 5;

    /** 平台收款账户：用户购票时票款进入该账户钱包（对应 t_user.id） */
    public static final Long PLATFORM_USER_ID = 1L;

    /* ========== 钱包零钱流水 ========== */
    /** 钱包流水类型：充值 */
    public static final int WALLET_FLOW_RECHARGE = 1;
    /** 钱包流水类型：消费 */
    public static final int WALLET_FLOW_CONSUME = 2;
    /** 钱包流水类型：退款 */
    public static final int WALLET_FLOW_REFUND = 3;
    /** 钱包流水类型：平台售票收入（用户购票时进入平台账户） */
    public static final int WALLET_FLOW_PLATFORM_INCOME = 4;

    /**
     * 支付单状态文案。
     */
    public static String payStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case PAY_STATUS_PAYING:
                return "支付中";
            case PAY_STATUS_SUCCESS:
                return "支付成功";
            case PAY_STATUS_FAILED:
                return "支付失败";
            case PAY_STATUS_CLOSED:
                return "已关闭";
            default:
                return "未知";
        }
    }

    /**
     * 钱包流水类型文案。
     */
    public static String walletFlowTypeName(Integer type) {
        if (type == null) {
            return "未知";
        }
        switch (type) {
            case WALLET_FLOW_RECHARGE:
                return "充值";
            case WALLET_FLOW_CONSUME:
                return "消费";
            case WALLET_FLOW_REFUND:
                return "退款";
            case WALLET_FLOW_PLATFORM_INCOME:
                return "平台售票收入";
            default:
                return "未知";
        }
    }
}

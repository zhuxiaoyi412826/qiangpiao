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
    /** 秒杀区间库存：按「相邻站单段」扣减，OD 区间需覆盖的每一段都够票才成交 */
    public static final String SECKILL_SEG_STOCK_KEY = "qp:seckill:seg:";
    /** 一人一单标记 */
    public static final String SECKILL_USER_KEY = "qp:seckill:user:";
    /** 用户抢票结果（订单号） */
    public static final String SECKILL_RESULT_KEY = "qp:seckill:result:";
    /** 批量抢票：批次号 -> 该批次全部订单号（逗号分隔） */
    public static final String SECKILL_BATCH_KEY = "qp:seckill:batch:";
    /** 批量抢票：单张票的下单结果（0 排队 / 1:车厢:座位 成功 / -1:原因 失败） */
    public static final String SECKILL_TICKET_KEY = "qp:seckill:ticket:";
    /** 批量抢票：批次 -> 已落位车厢（同批次后续票优先坐同一车厢） */
    public static final String SECKILL_BATCH_CARRIAGE_KEY = "qp:seckill:batch:carriage:";
    /** 秒杀排队：车次 + 席别 待处理票数（受理时 +N，落库成功 / 补偿时 -1） */
    public static final String SECKILL_QUEUE_PENDING_KEY = "qp:seckill:queue:pending:";
    /** 秒杀排队：车次 + 席别 累计受理序号（自增，用于告诉用户「你是第几位」） */
    public static final String SECKILL_QUEUE_SEQ_KEY = "qp:seckill:queue:seq:";
    /** 接口限流：用户维度（滑动窗口） */
    public static final String LIMIT_KEY = "qp:limit:user:";
    /** 接口限流：IP 维度（滑动窗口） */
    public static final String LIMIT_IP_KEY = "qp:limit:ip:";
    /** 接口限流：全局维度（令牌桶，单 key） */
    public static final String LIMIT_GLOBAL_KEY = "qp:limit:global";
    /** IP 黑名单（SET，值为 IP） */
    public static final String LIMIT_BLACK_IP_SET = "qp:limit:blacklist";
    /** 图形验证码：captchaId -> 验证码文本 */
    public static final String CAPTCHA_KEY = "qp:captcha:code:";
    /** 滑块验证码：sliderId -> 目标 x 坐标 */
    public static final String CAPTCHA_SLIDER_KEY = "qp:captcha:slider:";
    /** 风控：同一 IP 关联过的账号集合 */
    public static final String RISK_IP_USERS_KEY = "qp:risk:ip:users:";
    /** 风控：用户上一次抢票时间戳（毫秒） */
    public static final String RISK_LAST_AT_KEY = "qp:risk:last:";
    /** 风控：命中计数（IP / 用户维度） */
    public static final String RISK_MARK_KEY = "qp:risk:mark:";
    /** 风控事件流水（后台可查最近 N 条） */
    public static final String RISK_EVENTS_KEY = "qp:risk:events";
    /** 支付回调 nonce 防重放 */
    public static final String PAY_NOTIFY_NONCE_KEY = "qp:pay:notify:nonce:";
    /** 分布式锁 */
    public static final String LOCK_KEY = "qp:lock:";
    /** JWT 黑名单：jti -> 失效时间戳（登出 / 强制下线 / 封号），TTL = token 剩余有效期 */
    public static final String TOKEN_BLACKLIST_KEY = "qp:token:blacklist:";
    /** 用户当前有效 token 的 jti 集合（Set）：支持多端登录，踢下线时批量拉黑 */
    public static final String USER_TOKEN_KEY = "qp:token:user:";
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
    /** 新用户注册礼包：幂等键前缀（t_wallet_flow.idempotent_key 唯一索引保证只送一次） */
    public static final String WALLET_GIFT_IDEMPOTENT_PREFIX = "REGISTER_GIFT:";
    /** 新用户注册礼包：业务单号前缀 */
    public static final String WALLET_GIFT_BIZ_NO_PREFIX = "INIT";

    /* ========== 秒杀下单任务队列（Redis Stream） ========== */
    /** 任务流 key：XADD / XREADGROUP / XACK 都用它 */
    public static final String SECKILL_TASK_STREAM_KEY = "qp:seckill:task:stream";
    /** 消费组名 */
    public static final String SECKILL_TASK_GROUP = "qp-seckill-order";
    /** 任务终结标记前缀（防重复回退排队计数） */
    public static final String SECKILL_TASK_DONE_KEY = "qp:seckill:task:done:";

    /* ========== 库存对账 ========== */
    /** 库存漂移观察计数前缀：同一处漂移连续命中 N 次才自动校准，避免把「在途瞬间差」误当漂移修掉 */
    public static final String STOCK_DRIFT_KEY = "qp:stock:drift:";

    /* ========== 资金对账（t_recon_bill） ========== */
    /** 单据状态：待审核（等待后台人员处理） */
    public static final int RECON_STATUS_PENDING = 0;
    /** 单据状态：审核通过（已按 handle_action 执行退补账） */
    public static final int RECON_STATUS_APPROVED = 1;
    /** 单据状态：已驳回（判定为正常业务，不处理） */
    public static final int RECON_STATUS_REJECTED = 2;
    /** 单据状态：已关闭（差异自行恢复：下一轮对账发现已一致） */
    public static final int RECON_STATUS_CLOSED = 3;

    /** 业务域：支付单 */
    public static final String RECON_BIZ_PAYMENT = "PAYMENT";
    /** 业务域：订单 */
    public static final String RECON_BIZ_ORDER = "ORDER";
    /** 业务域：退款 / 退票 */
    public static final String RECON_BIZ_REFUND = "REFUND";
    /** 业务域：钱包（余额 vs 流水） */
    public static final String RECON_BIZ_WALLET = "WALLET";
    /** 业务域：平台收入（资金闭环） */
    public static final String RECON_BIZ_PLATFORM = "PLATFORM";

    /** 差异：支付单已成功，但没有对应的消费流水（钱没扣 / 流水漏记） */
    public static final String RECON_DIFF_PAY_NO_FLOW = "PAY_NO_FLOW";
    /** 差异：支付单金额与消费流水金额不一致 */
    public static final String RECON_DIFF_PAY_AMOUNT = "PAY_AMOUNT_DIFF";
    /** 差异：同一订单有多笔消费流水（疑似重复扣款） */
    public static final String RECON_DIFF_PAY_MULTI_FLOW = "PAY_MULTI_FLOW";
    /** 差异：订单已支付，但没有成功的支付单 */
    public static final String RECON_DIFF_ORDER_NO_PAYMENT = "ORDER_NO_PAYMENT";
    /** 差异：订单已退票，但没有退款流水（用户没收到退款） */
    public static final String RECON_DIFF_REFUND_NO_FLOW = "REFUND_NO_FLOW";
    /** 差异：订单退款金额与退款流水金额不一致 */
    public static final String RECON_DIFF_REFUND_AMOUNT = "REFUND_AMOUNT_DIFF";
    /** 差异：钱包余额与流水累计不一致（余额快照链断裂） */
    public static final String RECON_DIFF_WALLET_BALANCE = "WALLET_BALANCE_DIFF";
    /** 差异：钱包余额为负（透支，不该发生） */
    public static final String RECON_DIFF_WALLET_NEGATIVE = "WALLET_NEGATIVE";
    /** 差异：用户消费合计与平台收入合计不一致（平台入账失败被吞的典型表现） */
    public static final String RECON_DIFF_PLATFORM_INCOME = "PLATFORM_INCOME_DIFF";

    /** 处理动作：仅记录，人工线下处理 */
    public static final String RECON_ACTION_NONE = "NONE";
    /** 处理动作：退钱给用户（补退 / 退多扣的部分） */
    public static final String RECON_ACTION_REFUND_TO_USER = "REFUND_TO_USER";
    /** 处理动作：向用户补扣（收了票没扣到钱） */
    public static final String RECON_ACTION_CHARGE_USER = "CHARGE_USER";
    /** 处理动作：平台账户冲正（按差异正负自动补记收入或冲退） */
    public static final String RECON_ACTION_FIX_PLATFORM = "FIX_PLATFORM";

    /** 风险等级：低 */
    public static final int RECON_RISK_LOW = 1;
    /** 风险等级：中 */
    public static final int RECON_RISK_MIDDLE = 2;
    /** 风险等级：高（金额大 / 余额为负 / 重复扣款） */
    public static final int RECON_RISK_HIGH = 3;

    /** 来源：定时任务扫描发现 */
    public static final String RECON_SOURCE_JOB = "JOB";
    /** 来源：后台人工开单 */
    public static final String RECON_SOURCE_MANUAL = "MANUAL";
    /** 对账单号前缀 */
    public static final String RECON_BILL_NO_PREFIX = "RB";

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
     * 对账单据状态文案。
     */
    public static String reconStatusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case RECON_STATUS_PENDING:
                return "待审核";
            case RECON_STATUS_APPROVED:
                return "审核通过";
            case RECON_STATUS_REJECTED:
                return "已驳回";
            case RECON_STATUS_CLOSED:
                return "已关闭";
            default:
                return "未知";
        }
    }

    /**
     * 对账差异类型文案。
     */
    public static String reconDiffTypeText(String diffType) {
        if (diffType == null) {
            return "未知";
        }
        switch (diffType) {
            case RECON_DIFF_PAY_NO_FLOW:
                return "支付成功但无消费流水";
            case RECON_DIFF_PAY_AMOUNT:
                return "支付金额与流水不一致";
            case RECON_DIFF_PAY_MULTI_FLOW:
                return "同一订单多笔消费流水";
            case RECON_DIFF_ORDER_NO_PAYMENT:
                return "订单已支付但无成功支付单";
            case RECON_DIFF_REFUND_NO_FLOW:
                return "订单已退票但无退款流水";
            case RECON_DIFF_REFUND_AMOUNT:
                return "退款金额与流水不一致";
            case RECON_DIFF_WALLET_BALANCE:
                return "钱包余额与流水累计不一致";
            case RECON_DIFF_WALLET_NEGATIVE:
                return "钱包余额为负";
            case RECON_DIFF_PLATFORM_INCOME:
                return "用户消费与平台收入不一致";
            default:
                return diffType;
        }
    }

    /**
     * 对账处理动作文案。
     */
    public static String reconActionText(String action) {
        if (action == null) {
            return "仅记录";
        }
        switch (action) {
            case RECON_ACTION_REFUND_TO_USER:
                return "退钱给用户";
            case RECON_ACTION_CHARGE_USER:
                return "向用户补扣";
            case RECON_ACTION_FIX_PLATFORM:
                return "平台账户冲正";
            default:
                return "仅记录";
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

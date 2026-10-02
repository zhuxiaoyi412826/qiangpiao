package com.qiangpiao.common.constant;

/**
 * Redis key 构建工具：统一 key 规范，避免各服务散落拼串。
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static String trainList(String from, String to, String date) {
        return Constants.CACHE_TRAIN_LIST + from + "-" + to + "-" + date;
    }

    public static String trainDetail(Long trainId) {
        return Constants.CACHE_TRAIN_DETAIL + trainId;
    }

    public static String seatMap(Long trainId, Integer seatType) {
        return Constants.CACHE_SEAT_MAP + trainId + ":" + seatType;
    }

    public static String orderDetail(String orderNo) {
        return Constants.CACHE_ORDER_DETAIL + orderNo;
    }

    /** 秒杀库存 key：trainId + seatType */
    public static String seckillStock(Long trainId, Integer seatType) {
        return Constants.STOCK_KEY + trainId + ":" + seatType;
    }

    /** 秒杀区间库存 key：trainId + seatType + 段序号 */
    public static String seckillSegStock(Long trainId, Integer seatType, int segIndex) {
        return Constants.SECKILL_SEG_STOCK_KEY + trainId + ":" + seatType + ":" + segIndex;
    }

    /**
     * 限购额度 key：按「车次 + 用户」维度（不分席别），值是已占额度计数器，上限 9 张
     * （PurchaseLimitService.MAX_TICKETS_PER_TRAIN），减到 0 才删 key。
     */
    public static String seckillUser(Long trainId, Long userId) {
        return Constants.SECKILL_USER_KEY + trainId + ":" + userId;
    }

    /** 抢票结果 key（订单号） */
    public static String seckillResult(Long trainId, Integer seatType, Long userId) {
        return Constants.SECKILL_RESULT_KEY + trainId + ":" + seatType + ":" + userId;
    }

    /** 批量抢票：批次 key（value = 该批次订单号，逗号分隔） */
    public static String seckillBatch(String batchNo) {
        return Constants.SECKILL_BATCH_KEY + batchNo;
    }

    /** 批量抢票：批次内单张票的结果 key */
    public static String seckillTicket(String orderNo) {
        return Constants.SECKILL_TICKET_KEY + orderNo;
    }

    /** 批量抢票：批次已落位车厢（同批次的票优先分配同一车厢） */
    public static String seckillBatchCarriage(String batchNo) {
        return Constants.SECKILL_BATCH_CARRIAGE_KEY + batchNo;
    }

    /** 秒杀排队：待处理票数（trainId + seatType 维度） */
    public static String seckillQueuePending(Long trainId, Integer seatType) {
        return Constants.SECKILL_QUEUE_PENDING_KEY + trainId + ":" + seatType;
    }

    /** 秒杀排队：累计受理序号（trainId + seatType 维度） */
    public static String seckillQueueSeq(Long trainId, Integer seatType) {
        return Constants.SECKILL_QUEUE_SEQ_KEY + trainId + ":" + seatType;
    }

    /** 限流 key：用户维度（滑动窗口） */
    public static String userLimit(Long userId) {
        return Constants.LIMIT_KEY + userId;
    }

    /** 限流 key：IP 维度（滑动窗口） */
    public static String ipLimit(String ip) {
        return Constants.LIMIT_IP_KEY + ip;
    }

    /** 限流 key：全局维度（令牌桶，单 key） */
    public static String globalLimit() {
        return Constants.LIMIT_GLOBAL_KEY;
    }

    /** IP 黑名单集合 */
    public static String blackIpSet() {
        return Constants.LIMIT_BLACK_IP_SET;
    }

    /** IP 黑名单明细（Hash：IP -> 原因 + 解封时间） */
    public static String blackIpDetail() {
        return Constants.LIMIT_BLACK_IP_SET + ":detail";
    }

    /** 图形验证码 key */
    public static String captcha(String captchaId) {
        return Constants.CAPTCHA_KEY + captchaId;
    }

    /** 滑块验证码 key */
    public static String slider(String sliderId) {
        return Constants.CAPTCHA_SLIDER_KEY + sliderId;
    }

    /** 风控：同一 IP 关联账号集合 */
    public static String riskIpUsers(String ip) {
        return Constants.RISK_IP_USERS_KEY + ip;
    }

    /** 风控：用户上次抢票时间 */
    public static String riskLastAt(Long userId) {
        return Constants.RISK_LAST_AT_KEY + userId;
    }

    /** 风控：命中计数（传 ip 或 userId 字符串） */
    public static String riskMark(String dim) {
        return Constants.RISK_MARK_KEY + dim;
    }

    /** 风控事件流水 */
    public static String riskEvents() {
        return Constants.RISK_EVENTS_KEY;
    }

    /** 支付回调 nonce：防重放（同一 nonce 只处理一次） */
    public static String payNotifyNonce(String nonce) {
        return Constants.PAY_NOTIFY_NONCE_KEY + nonce;
    }

    /** 分布式锁 key */
    public static String lock(String bizKey) {
        return Constants.LOCK_KEY + bizKey;
    }

    /** JWT 黑名单：已登出 / 被强制下线的 token（jti），TTL = token 剩余有效期 */
    public static String tokenBlacklist(String jti) {
        return Constants.TOKEN_BLACKLIST_KEY + jti;
    }

    /** 某用户当前有效的 token jti 集合（Set）：用于「踢下线」时批量拉黑 */
    public static String userTokens(Long userId) {
        return Constants.USER_TOKEN_KEY + userId;
    }

    /** 秒杀下单任务队列（Redis Stream）：可靠异步落库，进程重启不丢任务 */
    public static String seckillTaskStream() {
        return Constants.SECKILL_TASK_STREAM_KEY;
    }

    /** 任务终结标记：排队计数只回退一次（重复投递 / 重试不再重复减） */
    public static String seckillTaskDone(String orderNo) {
        return Constants.SECKILL_TASK_DONE_KEY + orderNo;
    }

    /** 库存漂移观察计数（trainId + seatType + 维度）：连续命中阈值次才自动校准 */
    public static String stockDrift(Long trainId, Integer seatType, String dimension) {
        return Constants.STOCK_DRIFT_KEY + trainId + ":" + seatType + ":" + dimension;
    }
}

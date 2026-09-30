package com.qiangpiao.service;

import java.util.Map;
import java.util.Set;

/**
 * 限流服务：用户 / IP / 全局三个维度 + IP 黑名单。
 *
 * <pre>
 *   用户维度：滑动窗口（Redis ZSET + Lua），窗口内最多 user-limit 次
 *   IP 维度  ：滑动窗口，防止同一出口 IP 批量刷接口
 *   全局维度：令牌桶（Lua），容量内允许突发，长期速率被 refill 限制
 *   黑名单  ：Redis SET，命中直接拒绝（拦截器层已拦一道，业务层再确认一次）
 *
 *   Redis 异常时一律降级放行，避免缓存故障导致整个站点不可用。
 * </pre>
 */
public interface RateLimitService {

    /**
     * 滑动窗口限流。
     *
     * @param key      窗口 key
     * @param limit    窗口内允许的最大请求数
     * @param windowMs 窗口长度（毫秒）
     * @return true 放行
     */
    boolean slidingWindow(String key, int limit, long windowMs);

    /**
     * 令牌桶限流。
     *
     * @param key          桶 key
     * @param capacity     桶容量（允许的突发量）
     * @param refillPerSec 每秒补充的令牌数（长期速率）
     * @param requested    本次消耗令牌数
     * @return true 放行
     */
    boolean tokenBucket(String key, int capacity, int refillPerSec, int requested);

    /**
     * 抢票前置限流：全局 → IP → 用户，任一超限抛业务异常。
     *
     * @param userId 用户 ID（可为空）
     * @param ip     客户端 IP（可为空）
     */
    void assertSeckillAllowed(Long userId, String ip);

    /** 是否在 IP 黑名单中 */
    boolean isBlackIp(String ip);

    /**
     * 拉黑 IP。
     *
     * @param ip      IP
     * @param seconds 拉黑时长（秒）
     * @param reason  原因（后台可看）
     */
    void blockIp(String ip, long seconds, String reason);

    /** 解除拉黑 */
    void unblockIp(String ip);

    /** 全部黑名单 IP */
    Set<String> blackIps();

    /** 黑名单明细：IP -> 拉黑原因 */
    Map<String, String> blackIpDetails();
}

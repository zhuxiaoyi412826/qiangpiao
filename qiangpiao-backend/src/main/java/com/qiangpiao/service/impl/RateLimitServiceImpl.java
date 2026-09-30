package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 限流实现：滑动窗口（用户 / IP）+ 令牌桶（全局）+ IP 黑名单。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    /** 黑名单默认保留时长（秒）：拉黑时未指定时长兜底用 */
    private static final long DEFAULT_BLOCK_SECONDS = 30 * 60;

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("slidingWindowScript")
    private final RedisScript<Long> slidingWindowScript;
    @Qualifier("tokenBucketScript")
    private final RedisScript<Long> tokenBucketScript;

    /** 用户维度：窗口内最多几次 */
    @Value("${seckill.user-limit:20}")
    private int userLimit;
    @Value("${seckill.user-window-seconds:60}")
    private int userWindowSeconds;
    /** IP 维度：窗口内最多几次 */
    @Value("${seckill.ip-limit:30}")
    private int ipLimit;
    @Value("${seckill.ip-window-seconds:60}")
    private int ipWindowSeconds;
    /** 全局维度：令牌桶容量与每秒补充量 */
    @Value("${seckill.global-capacity:5000}")
    private int globalCapacity;
    @Value("${seckill.global-refill:1000}")
    private int globalRefill;

    @Override
    public boolean slidingWindow(String key, int limit, long windowMs) {
        try {
            Long allowed = stringRedisTemplate.execute(slidingWindowScript, Collections.singletonList(key),
                    String.valueOf(windowMs), String.valueOf(System.currentTimeMillis()), String.valueOf(limit));
            return Long.valueOf(1L).equals(allowed);
        } catch (Exception e) {
            log.warn("滑动窗口限流失败，降级放行：key={}, msg={}", key, e.getMessage());
            return true;
        }
    }

    @Override
    public boolean tokenBucket(String key, int capacity, int refillPerSec, int requested) {
        try {
            long ttl = refillPerSec <= 0 ? 60 : (capacity / Math.max(refillPerSec, 1)) + 10;
            Long allowed = stringRedisTemplate.execute(tokenBucketScript, Collections.singletonList(key),
                    String.valueOf(capacity), String.valueOf(refillPerSec),
                    String.valueOf(System.currentTimeMillis()), String.valueOf(requested),
                    String.valueOf(ttl));
            return Long.valueOf(1L).equals(allowed);
        } catch (Exception e) {
            log.warn("令牌桶限流失败，降级放行：key={}, msg={}", key, e.getMessage());
            return true;
        }
    }

    @Override
    public void assertSeckillAllowed(Long userId, String ip) {
        // 1) 黑名单：直接拒绝
        if (StringUtils.hasText(ip) && isBlackIp(ip)) {
            throw new BizException(ResultCode.IP_BLOCKED);
        }
        // 2) 全局：令牌桶，保护整个系统不被打垮
        if (!tokenBucket(RedisKeys.globalLimit(), globalCapacity, globalRefill, 1)) {
            log.warn("全局限流触发：capacity={}, refill={}/s", globalCapacity, globalRefill);
            throw new BizException(ResultCode.SYSTEM_BUSY_GLOBAL);
        }
        // 3) IP 维度：同一出口 IP 的批量请求
        if (StringUtils.hasText(ip)
                && !slidingWindow(RedisKeys.ipLimit(ip), ipLimit, ipWindowSeconds * 1000L)) {
            log.warn("IP 限流触发：ip={}, limit={}/{}s", ip, ipLimit, ipWindowSeconds);
            throw new BizException(ResultCode.IP_TOO_MANY_REQUESTS);
        }
        // 4) 用户维度：单用户窗口内次数
        if (userId != null
                && !slidingWindow(RedisKeys.userLimit(userId), userLimit, userWindowSeconds * 1000L)) {
            log.warn("用户限流触发：userId={}, limit={}/{}s", userId, userLimit, userWindowSeconds);
            throw new BizException(ResultCode.TOO_MANY_REQUESTS);
        }
    }

    @Override
    public boolean isBlackIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(RedisKeys.blackIpSet(), ip));
        } catch (Exception e) {
            log.warn("黑名单查询失败，按非黑名单处理：ip={}, msg={}", ip, e.getMessage());
            return false;
        }
    }

    @Override
    public void blockIp(String ip, long seconds, String reason) {
        if (!StringUtils.hasText(ip)) {
            return;
        }
        long ttl = seconds > 0 ? seconds : DEFAULT_BLOCK_SECONDS;
        try {
            stringRedisTemplate.opsForSet().add(RedisKeys.blackIpSet(), ip);
            String detail = (reason == null ? "手动拉黑" : reason)
                    + " | 解封时间 " + java.time.LocalDateTime.now().plusSeconds(ttl)
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            stringRedisTemplate.opsForHash().put(RedisKeys.blackIpDetail(), ip, detail);
            // 黑名单集合本身不过期，靠明细的 TTL 做定时清理依据（后台可手动解封）
            log.warn("IP 已拉黑：ip={}, 时长={}s, 原因={}", ip, ttl, reason);
        } catch (Exception e) {
            log.error("拉黑失败：ip={}, msg={}", ip, e.getMessage());
        }
    }

    @Override
    public void unblockIp(String ip) {
        if (!StringUtils.hasText(ip)) {
            return;
        }
        try {
            stringRedisTemplate.opsForSet().remove(RedisKeys.blackIpSet(), ip);
            stringRedisTemplate.opsForHash().delete(RedisKeys.blackIpDetail(), ip);
            stringRedisTemplate.delete(RedisKeys.riskMark(ip));
            log.info("IP 已解除拉黑：ip={}", ip);
        } catch (Exception e) {
            log.error("解除拉黑失败：ip={}, msg={}", ip, e.getMessage());
        }
    }

    @Override
    public Set<String> blackIps() {
        try {
            Set<String> members = stringRedisTemplate.opsForSet().members(RedisKeys.blackIpSet());
            return members == null ? Collections.emptySet() : members;
        } catch (Exception e) {
            log.warn("黑名单列表查询失败：msg={}", e.getMessage());
            return Collections.emptySet();
        }
    }

    @Override
    public Map<String, String> blackIpDetails() {
        try {
            Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(RedisKeys.blackIpDetail());
            Map<String, String> result = new java.util.LinkedHashMap<>();
            entries.forEach((k, v) -> result.put(String.valueOf(k), String.valueOf(v)));
            return result;
        } catch (Exception e) {
            log.warn("黑名单明细查询失败：msg={}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}

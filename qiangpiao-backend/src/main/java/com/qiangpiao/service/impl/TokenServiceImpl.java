package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.util.JwtTokenUtil;
import com.qiangpiao.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单实现（Redis）。
 * <pre>
 *   qp:token:blacklist:{jti}  -> 失效时间戳，TTL = token 剩余有效期（过期自动清理）
 *   qp:token:user:{userId}    -> Set，该用户当前有效 token 的 jti（多端登录）
 * </pre>
 * 设计取舍：黑名单只存「需要失效」的 token，且 TTL 跟着 token 剩余寿命走，
 * 因此不会无限膨胀；Redis 不可用时降级为「不失效」，保证可用性优先。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenUtil jwtTokenUtil;

    @Value("${jwt.expiration}")
    private long expirationSeconds;

    @Override
    public void register(Long userId, String token) {
        String jti = jwtTokenUtil.getJti(token);
        if (userId == null || jti == null) {
            return;
        }
        long ttl = ttlOf(token);
        try {
            String key = RedisKeys.userTokens(userId);
            stringRedisTemplate.opsForSet().add(key, jti);
            // 集合 TTL 只增不减：新登录的 token 比老的有效期更长时，别把老的记录提前清掉
            Long oldTtl = stringRedisTemplate.getExpire(key);
            if (oldTtl == null || oldTtl < ttl) {
                stringRedisTemplate.expire(key, ttl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("token 登记失败，降级为不登记（不影响登录）：userId={}, msg={}", userId, e.getMessage());
        }
    }

    @Override
    public boolean blacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(RedisKeys.tokenBlacklist(jti)));
        } catch (Exception e) {
            // Redis 故障时不拦截：可用性优先，否则缓存抖动会导致全站掉线
            log.warn("token 黑名单查询失败，降级放行：msg={}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean blacklistToken(String token) {
        String jti = jwtTokenUtil.getJti(token);
        if (jti == null) {
            // 改造前签发的老 token 没有 jti，无法精准失效，等它自然过期即可
            log.info("token 无 jti，跳过拉黑（老 token 将按原有效期失效）");
            return false;
        }
        long ttl = ttlOf(token);
        try {
            stringRedisTemplate.opsForValue().set(RedisKeys.tokenBlacklist(jti),
                    String.valueOf(System.currentTimeMillis()), ttl, TimeUnit.SECONDS);
            Long userId = jwtTokenUtil.getUserId(token);
            if (userId != null) {
                stringRedisTemplate.opsForSet().remove(RedisKeys.userTokens(userId), jti);
            }
            log.info("token 已拉黑：userId={}, jti={}, ttl={}s", userId, jti, ttl);
            return true;
        } catch (Exception e) {
            log.error("token 拉黑失败：jti={}, msg={}", jti, e.getMessage());
            return false;
        }
    }

    @Override
    public int kickUser(Long userId) {
        if (userId == null) {
            return 0;
        }
        try {
            Set<String> jtis = stringRedisTemplate.opsForSet().members(RedisKeys.userTokens(userId));
            if (jtis == null || jtis.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (String jti : jtis) {
                if (jti == null || jti.isEmpty()) {
                    continue;
                }
                // 单端 TTL 已无从得知（只存了 jti），按配置的有效期上限写，宁长勿短
                stringRedisTemplate.opsForValue().set(RedisKeys.tokenBlacklist(jti),
                        String.valueOf(System.currentTimeMillis()),
                        Math.max(expirationSeconds, 1), TimeUnit.SECONDS);
                count++;
            }
            stringRedisTemplate.delete(RedisKeys.userTokens(userId));
            log.info("用户已强制下线：userId={}, 失效 token 数={}", userId, count);
            return count;
        } catch (Exception e) {
            log.error("强制下线失败：userId={}, msg={}", userId, e.getMessage());
            return 0;
        }
    }

    /** 拉黑 TTL：token 剩余有效期；已过期则给 1 秒（避免写入永久 key） */
    private long ttlOf(String token) {
        long remaining = jwtTokenUtil.remainingSeconds(token);
        return remaining <= 0 ? 1 : remaining;
    }
}

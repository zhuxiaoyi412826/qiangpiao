package com.qiangpiao.service.impl;

import com.qiangpiao.bo.RiskCheckBO;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.service.RateLimitService;
import com.qiangpiao.service.RiskControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 风控实现：IP 多账号 + 极短耗时 + 累计自动拉黑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskControlServiceImpl implements RiskControlService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 同一 IP 关联账号的统计窗口（分钟） */
    private static final long IP_WINDOW_MINUTES = 30;
    /** 命中计数的统计窗口（分钟） */
    private static final long MARK_WINDOW_MINUTES = 60;
    /** 风控事件保留条数 */
    private static final int EVENT_KEEP = 200;

    private final StringRedisTemplate stringRedisTemplate;
    private final RateLimitService rateLimitService;

    /** 同一 IP 关联账号数阈值 */
    @Value("${risk.ip-multi-account:3}")
    private int ipMultiAccountThreshold;
    /** 两次抢票最小间隔（毫秒），低于此值视为脚本 */
    @Value("${risk.min-interval-ms:500}")
    private long minIntervalMs;
    /** 累计命中达到该次数自动拉黑 */
    @Value("${risk.auto-block-count:5}")
    private int autoBlockCount;
    /** 自动拉黑时长（秒） */
    @Value("${risk.block-seconds:1800}")
    private long blockSeconds;

    @Override
    public RiskCheckBO check(Long userId, String ip) {
        List<String> reasons = new ArrayList<>();
        long now = System.currentTimeMillis();

        // 1) 同一 IP 多账号
        if (StringUtils.hasText(ip) && userId != null) {
            try {
                String setKey = RedisKeys.riskIpUsers(ip);
                stringRedisTemplate.opsForSet().add(setKey, String.valueOf(userId));
                stringRedisTemplate.expire(setKey, IP_WINDOW_MINUTES, TimeUnit.MINUTES);
                Long accounts = stringRedisTemplate.opsForSet().size(setKey);
                if (accounts != null && accounts > ipMultiAccountThreshold) {
                    reasons.add("同一 IP " + ip + " 在 " + IP_WINDOW_MINUTES + " 分钟内关联 "
                            + accounts + " 个账号（阈值 " + ipMultiAccountThreshold + "）");
                }
            } catch (Exception e) {
                log.warn("IP 多账号统计失败：ip={}, msg={}", ip, e.getMessage());
            }

            // 2) 极短耗时请求
            try {
                String lastKey = RedisKeys.riskLastAt(userId);
                String last = stringRedisTemplate.opsForValue().get(lastKey);
                if (StringUtils.hasText(last)) {
                    long interval = now - Long.parseLong(last);
                    if (interval < minIntervalMs) {
                        reasons.add("距上次抢票仅 " + interval + " ms（阈值 " + minIntervalMs + " ms），疑似脚本");
                    }
                }
                stringRedisTemplate.opsForValue().set(lastKey, String.valueOf(now), 10, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("请求间隔统计失败：userId={}, msg={}", userId, e.getMessage());
            }
        }

        boolean hit = !reasons.isEmpty();
        long hits = 0;
        boolean blocked = false;
        if (hit) {
            String detail = String.join("；", reasons);
            hits = incrHits(StringUtils.hasText(ip) ? ip : "user:" + userId);
            record(ip, userId, detail + "，累计命中 " + hits + " 次");
            if (StringUtils.hasText(ip) && hits >= autoBlockCount) {
                rateLimitService.blockIp(ip, blockSeconds, "风控自动拉黑：" + detail);
                blocked = true;
                log.warn("风控自动拉黑：ip={}, 累计命中={}", ip, hits);
            }
        }
        return RiskCheckBO.builder().hit(hit).reasons(reasons).hits(hits).blocked(blocked).build();
    }

    @Override
    public void record(String ip, Long userId, String detail) {
        String dim = StringUtils.hasText(ip) ? ip : "user:" + userId;
        String event = LocalDateTime.now().format(FMT) + " | " + dim
                + " | userId=" + userId + " | " + detail;
        try {
            stringRedisTemplate.opsForList().leftPush(RedisKeys.riskEvents(), event);
            stringRedisTemplate.opsForList().trim(RedisKeys.riskEvents(), 0, EVENT_KEEP - 1);
        } catch (Exception e) {
            log.warn("风控事件写入失败：msg={}", e.getMessage());
        }
        log.warn("风控命中：{}", event);
    }

    @Override
    public List<String> recentEvents(int limit) {
        try {
            int size = limit <= 0 ? 20 : Math.min(limit, EVENT_KEEP);
            List<String> list = stringRedisTemplate.opsForList().range(RedisKeys.riskEvents(), 0, size - 1);
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.warn("风控事件查询失败：msg={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public long hits(String dim) {
        try {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.riskMark(dim));
            return StringUtils.hasText(value) ? Long.parseLong(value) : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    public void resetHits(String dim) {
        try {
            stringRedisTemplate.delete(RedisKeys.riskMark(dim));
        } catch (Exception e) {
            log.warn("清空命中计数失败：dim={}, msg={}", dim, e.getMessage());
        }
    }

    /** 命中计数 +1，窗口内累计 */
    private long incrHits(String dim) {
        try {
            String key = RedisKeys.riskMark(dim);
            Long value = stringRedisTemplate.opsForValue().increment(key);
            if (value != null && value == 1L) {
                stringRedisTemplate.expire(key, MARK_WINDOW_MINUTES, TimeUnit.MINUTES);
            }
            return value == null ? 1L : value;
        } catch (Exception e) {
            log.warn("命中计数失败：dim={}, msg={}", dim, e.getMessage());
            return 1L;
        }
    }
}

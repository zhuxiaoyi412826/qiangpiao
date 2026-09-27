package com.qiangpiao.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiangpiao.common.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 三级缓存门面（读多写少场景，如车次查询 / 车站 / 座位图）：
 * <pre>
 *   L1：Caffeine 本地缓存（进程内，微秒级，抗热点）
 *   L2：Redis 分布式缓存（毫秒级，跨实例共享）
 *   L3：DB 数据库（兜底回源，并回写 L2 / L1）
 * </pre>
 * 内置防护：
 * <ul>
 *   <li>缓存穿透：DB 空结果写入占位值（短期 TTL）</li>
 *   <li>缓存击穿：本地锁 + 双重检查，单实例只有一个线程回源</li>
 *   <li>缓存雪崩：TTL 随机抖动</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiLevelCacheService {

    private final LocalCache localCache;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper cacheObjectMapper;

    /** 本地锁：保证单实例内同一 key 只有一个线程回源 */
    private final Map<String, ReentrantLock> localLocks = new ConcurrentHashMap<>();

    // ==================== 对外 API ====================

    public <T> T get(String key, Class<T> type, Supplier<T> dbLoader, long ttl) {
        return get(key, cacheObjectMapper.getTypeFactory().constructType(type), dbLoader, ttl);
    }

    public <T> T get(String key, TypeReference<T> typeRef, Supplier<T> dbLoader, long ttl) {
        return get(key, cacheObjectMapper.getTypeFactory().constructType(typeRef), dbLoader, ttl);
    }

    public <T> List<T> getList(String key, Class<T> elementType, Supplier<List<T>> dbLoader, long ttl) {
        JavaType javaType = cacheObjectMapper.getTypeFactory().constructCollectionType(List.class, elementType);
        return get(key, javaType, dbLoader, ttl);
    }

    public void put(String key, Object value, long ttl) {
        if (value == null) {
            stringRedisTemplate.opsForValue().set(key, Constants.CACHE_NULL_VALUE, 60, TimeUnit.SECONDS);
            return;
        }
        stringRedisTemplate.opsForValue().set(key, serialize(value), randomTtl(ttl), TimeUnit.SECONDS);
        localCache.put(key, value);
    }

    /**
     * 失效缓存（L1 + L2 双删）
     */
    public void evict(String key) {
        localCache.evict(key);
        stringRedisTemplate.delete(key);
    }

    /**
     * 按前缀批量失效（L1 过滤删除 + L2 SCAN 批量删除）。
     * 用于结构性数据变更（如车次日期整体滚动）后的缓存清理。
     */
    public void evictPrefix(String prefix) {
        localCache.evictPrefix(prefix);
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(prefix + "*").count(500).build())) {
            List<String> batch = new ArrayList<>();
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 500) {
                    stringRedisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                stringRedisTemplate.delete(batch);
            }
        } catch (Exception e) {
            log.warn("按前缀清理 Redis 缓存失败：prefix={}", prefix, e);
        }
    }

    // ==================== 核心流程 ====================

    private <T> T get(String key, JavaType javaType, Supplier<T> dbLoader, long ttl) {
        // ---------- L1：本地缓存 ----------
        Object l1 = localCache.get(key);
        if (l1 != null) {
            log.debug("命中 L1 缓存 key={}", key);
            return cast(l1);
        }

        // ---------- L2：Redis ----------
        String l2 = stringRedisTemplate.opsForValue().get(key);
        if (l2 != null) {
            if (Constants.CACHE_NULL_VALUE.equals(l2)) {
                return null;
            }
            T value = deserialize(l2, javaType);
            if (value != null) {
                localCache.put(key, value);
                log.debug("命中 L2 缓存并回填 L1 key={}", key);
            }
            return value;
        }

        // ---------- L3：回源 DB（加锁防击穿） ----------
        ReentrantLock lock = localLocks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            Object again = localCache.get(key);
            if (again != null) {
                return cast(again);
            }
            String againRedis = stringRedisTemplate.opsForValue().get(key);
            if (againRedis != null) {
                if (Constants.CACHE_NULL_VALUE.equals(againRedis)) {
                    return null;
                }
                T v = deserialize(againRedis, javaType);
                localCache.put(key, v);
                return v;
            }

            T dbValue = dbLoader.get();
            if (dbValue == null) {
                // 空值占位，防穿透
                stringRedisTemplate.opsForValue().set(key, Constants.CACHE_NULL_VALUE, 60, TimeUnit.SECONDS);
            } else {
                stringRedisTemplate.opsForValue().set(key, serialize(dbValue), randomTtl(ttl), TimeUnit.SECONDS);
                localCache.put(key, dbValue);
            }
            log.debug("回源 DB(L3) 并写入缓存 key={}", key);
            return dbValue;
        } finally {
            lock.unlock();
            localLocks.remove(key);
        }
    }

    // ==================== 工具方法 ====================

    @SuppressWarnings("unchecked")
    private <T> T cast(Object value) {
        return (T) value;
    }

    /** TTL 随机抖动，防止同一时间大面积过期造成雪崩 */
    private long randomTtl(long ttl) {
        if (ttl <= 0) {
            return 60L;
        }
        return ttl + (long) (Math.random() * ttl * 0.2);
    }

    private String serialize(Object value) {
        try {
            return cacheObjectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.error("缓存序列化失败", e);
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }

    private <T> T deserialize(String json, JavaType javaType) {
        try {
            return cacheObjectMapper.readValue(json, javaType);
        } catch (Exception e) {
            log.error("缓存反序列化失败，json={}", json, e);
            return null;
        }
    }
}

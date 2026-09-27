package com.qiangpiao.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * L1 本地缓存（Caffeine，进程内）：扛住秒杀场景下的热点读，减少 Redis 网络开销。
 */
@Slf4j
@Component
public class LocalCache {

    private final Cache<String, Object> cache;

    public LocalCache(long maxSize, long ttlSeconds) {
        this.cache = Caffeine.newBuilder()
                .initialCapacity(1024)
                .maximumSize(Math.max(maxSize, 1))
                .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                .recordStats()
                .removalListener((key, value, cause) ->
                        log.debug("L1缓存移除 key={}, cause={}", key, cause))
                .build();
        log.info("L1 本地缓存初始化完成：maxSize={}, ttl={}s", maxSize, ttlSeconds);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) cache.getIfPresent(key);
    }

    public void put(String key, Object value) {
        cache.put(key, value);
    }

    public void evict(String key) {
        cache.invalidate(key);
    }

    /**
     * 按前缀失效：用于结构性数据变更（如车次日期整体滚动）后统一清理 L1。
     */
    public void evictPrefix(String prefix) {
        cache.asMap().keySet().removeIf(key -> key != null && key.startsWith(prefix));
    }

    public void clear() {
        cache.invalidateAll();
    }

    public long estimatedSize() {
        return cache.estimatedSize();
    }
}

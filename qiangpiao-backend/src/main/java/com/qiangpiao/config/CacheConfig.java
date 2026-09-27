package com.qiangpiao.config;

import com.qiangpiao.cache.LocalCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缓存配置：L1 本地缓存参数。
 */
@Configuration
public class CacheConfig {

    @Value("${cache.local.max-size}")
    private long maxSize;
    @Value("${cache.local.ttl}")
    private long ttl;
    @Value("${cache.default-ttl}")
    private long defaultTtl;

    @Bean
    public LocalCache localCache() {
        return new LocalCache(maxSize, ttl);
    }

    @Bean("cacheDefaultTtl")
    public Long cacheDefaultTtl() {
        return defaultTtl;
    }
}

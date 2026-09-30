package com.qiangpiao.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis（L2 缓存 / 秒杀库存 / 分布式锁）配置。
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Value("${redis.host}")
    private String host;
    @Value("${redis.port}")
    private int port;
    @Value("${redis.password}")
    private String password;
    @Value("${redis.database}")
    private int database;
    @Value("${redis.timeout}")
    private long timeout;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration();
        standalone.setHostName(host);
        standalone.setPort(port);
        standalone.setDatabase(database);
        if (password != null && !password.trim().isEmpty()) {
            standalone.setPassword(password);
        }
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .shutdownTimeout(Duration.ofMillis(100))
                .build();
        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
        log.info("Redis 连接工厂初始化：{}:{} db={}", host, port, database);
        return factory;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        return template;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 对象映射器（缓存 JSON 序列化，支持 JDK8 时间类型）
     */
    @Bean
    public ObjectMapper cacheObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * 秒杀库存扣减脚本：库存不足返回 -1，扣减成功返回剩余库存。
     */
    @Bean
    public RedisScript<Long> seckillStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        // 一次可买多张：ARGV[1] 为本次张数（缺省按 1 处理）
        // 返回：-1 未初始化；-2 库存不足（剩余 < 本次张数，整批失败，不部分扣减）；其他 扣减后的剩余库存
        script.setScriptText(
                "local stock = redis.call('get', KEYS[1]) " +
                        "if (stock == false) then return -1 end " +
                        "local n = tonumber(ARGV[1]) " +
                        "if (n == nil or n < 1) then n = 1 end " +
                        "if (tonumber(stock) < n) then return -2 end " +
                        "return redis.call('decrby', KEYS[1], n)");
        return script;
    }

    /**
     * 回滚库存脚本（ARGV[1] = 张数，缺省 1）。
     */
    @Bean
    public RedisScript<Long> rollbackStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local n = tonumber(ARGV[1]) " +
                        "if (n == nil or n < 1) then n = 1 end " +
                        "return redis.call('incrby', KEYS[1], n)");
        return script;
    }

    /**
     * 区间票库存扣减脚本：一次抢票要占用 OD 区间覆盖的「每一段」库存。
     * <pre>
     *   KEYS = 该区间覆盖的所有单段 key（trainId + seatType + segIndex）
     *   ARGV[1] = 本次张数
     *   返回：1 成功 / -1 有段未初始化 / -2 有段余票不足（整批失败，不部分扣减）
     * </pre>
     * 先检查全部段、再统一扣减，Lua 原子执行，因此不会出现「扣了一半」的中间态。
     */
    @Bean
    public RedisScript<Long> seckillSegStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local n = tonumber(ARGV[1]) " +
                        "if (n == nil or n < 1) then n = 1 end " +
                        "for i = 1, #KEYS do " +
                        "  local v = redis.call('get', KEYS[i]) " +
                        "  if (v == false) then return -1 end " +
                        "  if (tonumber(v) < n) then return -2 end " +
                        "end " +
                        "for i = 1, #KEYS do " +
                        "  redis.call('decrby', KEYS[i], n) " +
                        "end " +
                        "return 1");
        return script;
    }

    /**
     * 区间票库存回滚脚本：与 {@link #seckillSegStockScript()} 配对，覆盖的每一段都归还。
     */
    @Bean
    public RedisScript<Long> rollbackSegStockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local n = tonumber(ARGV[1]) " +
                        "if (n == nil or n < 1) then n = 1 end " +
                        "for i = 1, #KEYS do " +
                        "  redis.call('incrby', KEYS[i], n) " +
                        "end " +
                        "return 1");
        return script;
    }

    /**
     * 令牌桶限流脚本（全局维度用）。
     * <pre>
     *   KEYS[1] = 桶 key
     *   ARGV[1] = 桶容量  ARGV[2] = 每秒补充令牌数  ARGV[3] = 当前毫秒
     *   ARGV[4] = 本次取几个令牌  ARGV[5] = key 过期秒数
     *   返回：1 放行 / 0 限流
     * </pre>
     * 令牌按时间差惰性补充，允许突发流量打满桶（capacity），长期速率被 refill 限制。
     */
    @Bean
    public RedisScript<Long> tokenBucketScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local capacity = tonumber(ARGV[1]) " +
                        "local refill = tonumber(ARGV[2]) " +
                        "local now = tonumber(ARGV[3]) " +
                        "local requested = tonumber(ARGV[4]) " +
                        "local ttl = tonumber(ARGV[5]) " +
                        "local data = redis.call('hmget', KEYS[1], 'tokens', 'ts') " +
                        "local tokens = tonumber(data[1]) " +
                        "local ts = tonumber(data[2]) " +
                        "if (tokens == nil or ts == nil) then " +
                        "  tokens = capacity " +
                        "  ts = now " +
                        "elseif (now > ts) then " +
                        "  tokens = math.min(capacity, tokens + (now - ts) / 1000.0 * refill) " +
                        "end " +
                        "local allowed = 0 " +
                        "if (tokens >= requested) then " +
                        "  tokens = tokens - requested " +
                        "  allowed = 1 " +
                        "end " +
                        "redis.call('hmset', KEYS[1], 'tokens', tostring(tokens), 'ts', tostring(now)) " +
                        "redis.call('expire', KEYS[1], ttl) " +
                        "return allowed");
        return script;
    }

    /**
     * 滑动窗口限流脚本（用户 / IP 维度用）。
     * <pre>
     *   KEYS[1] = 窗口 key
     *   ARGV[1] = 窗口长度（毫秒）  ARGV[2] = 当前毫秒  ARGV[3] = 窗口内最大请求数
     *   返回：1 放行 / 0 限流
     * </pre>
     * 用 ZSET 记录每次请求的时间戳，先清掉窗口外的旧数据再计数，窗口边界不会出现双倍放行。
     */
    @Bean
    public RedisScript<Long> slidingWindowScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "local key = KEYS[1] " +
                        "local window = tonumber(ARGV[1]) " +
                        "local now = tonumber(ARGV[2]) " +
                        "local limit = tonumber(ARGV[3]) " +
                        "redis.call('zremrangebyscore', key, 0, now - window) " +
                        "local count = redis.call('zcard', key) " +
                        "if (count < limit) then " +
                        "  local seq = redis.call('incr', key .. ':seq') " +
                        "  redis.call('zadd', key, now, tostring(now) .. '-' .. tostring(seq)) " +
                        "  redis.call('pexpire', key, window + 1000) " +
                        "  redis.call('pexpire', key .. ':seq', window + 1000) " +
                        "  return 1 " +
                        "end " +
                        "redis.call('pexpire', key, window + 1000) " +
                        "return 0");
        return script;
    }

    /**
     * 释放分布式锁脚本（只能释放自己持有的锁）。
     */
    @Bean
    public RedisScript<Long> unlockScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText(
                "if (redis.call('get', KEYS[1]) == ARGV[1]) then " +
                        "return redis.call('del', KEYS[1]) " +
                        "else return 0 end");
        return script;
    }
}

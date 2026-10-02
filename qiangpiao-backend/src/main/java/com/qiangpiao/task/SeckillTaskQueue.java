package com.qiangpiao.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.RedisKeys;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 秒杀下单任务队列（Redis Stream）。
 * <pre>
 *   生产者：XADD qp:seckill:task:stream * task &lt;json&gt;
 *   消费者：XREADGROUP GROUP qp-seckill-order &lt;consumer&gt; ... &gt;   （新消息）
 *           成功后 XACK；业务失败补偿后也 XACK（重试无意义）；
 *           系统异常不 XACK，留在 pending 等待 XCLAIM 重投。
 *   巡检：XPENDING 找出空闲超时的消息 → XCLAIM 抢过来重投；投递次数超限则补偿后确认。
 * </pre>
 * 相比原来的内存线程池：任务持久化在 Redis，进程重启 / 宕机都不会丢，
 * 也就不会出现「Redis 库存已扣、订单没落库、前端一直排队中」的悬空状态。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillTaskQueue {

    /** Stream 里承载任务 JSON 的字段名 */
    private static final String FIELD_TASK = "task";
    /** 占位消息字段：只为在 XGROUP CREATE 之前把 stream 建出来 */
    private static final String FIELD_INIT = "init";

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("cacheObjectMapper")
    private final ObjectMapper objectMapper;

    @Value("${seckill.task-batch-size:20}")
    private int batchSize;
    @Value("${seckill.task-block-ms:2000}")
    private long blockMs;
    @Value("${seckill.task-claim-idle-ms:30000}")
    private long claimIdleMs;
    @Value("${seckill.task-stream-max-len:100000}")
    private long maxLen;

    /**
     * 投递一条下单任务（持久化，返回消息 ID）。
     */
    public String enqueue(SeckillTaskBO taskBO) {
        Map<String, String> body = Collections.singletonMap(FIELD_TASK, write(taskBO));
        RecordId id = stringRedisTemplate.opsForStream()
                .add(StreamRecords.newRecord().ofMap(body).withStreamKey(RedisKeys.seckillTaskStream()));
        return id == null ? null : id.getValue();
    }

    /**
     * 读取新消息（阻塞）：只取本消费者组尚未投递过的（&gt;）。
     */
    public List<TaskMessage> readNew(String consumerName) {
        List<MapRecord<String, Object, Object>> records = stringRedisTemplate.opsForStream().read(
                Consumer.from(Constants.SECKILL_TASK_GROUP, consumerName),
                StreamReadOptions.empty().block(Duration.ofMillis(blockMs)).count(batchSize),
                StreamOffset.create(RedisKeys.seckillTaskStream(), ReadOffset.lastConsumed()));
        return toMessages(records, 1L);
    }

    /**
     * 巡检 pending：把「空闲超过阈值」的消息抢到本消费者名下重投。
     * 这是进程宕机后任务能被捞回来的关键路径。
     */
    @SuppressWarnings("unchecked")
    public List<TaskMessage> claimIdle(String consumerName) {
        String key = RedisKeys.seckillTaskStream();
        String group = Constants.SECKILL_TASK_GROUP;
        try {
            PendingMessages pending = stringRedisTemplate.opsForStream()
                    .pending(key, group, Range.unbounded(), (long) batchSize);
            Map<String, Long> deliveries = new HashMap<>();
            List<RecordId> ids = new ArrayList<>();
            for (PendingMessage pm : pending) {
                if (pm.getElapsedTimeSinceLastDelivery().toMillis() < claimIdleMs) {
                    continue;
                }
                deliveries.put(pm.getId().getValue(), Math.max(pm.getTotalDeliveryCount(), 1L));
                ids.add(pm.getId());
            }
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            List<MapRecord<String, Object, Object>> claimed = stringRedisTemplate.opsForStream().claim(
                    key, group, consumerName,
                    RedisStreamCommands.XClaimOptions.minIdle(Duration.ofMillis(claimIdleMs))
                            .ids(ids.toArray(new RecordId[0])));
            List<TaskMessage> result = new ArrayList<>(claimed.size());
            for (MapRecord<String, Object, Object> record : claimed) {
                String id = record.getId().getValue();
                result.add(new TaskMessage(id, read(record), deliveries.getOrDefault(id, 1L)));
            }
            log.warn("秒杀任务 pending 重投：{} 条（可能是上次进程异常退出遗留）", result.size());
            return result;
        } catch (Exception e) {
            // 组 / stream 不存在、Redis 抖动：本次巡检跳过，下个周期再来
            log.debug("巡检 pending 失败（下个周期重试）：msg={}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 消费确认：从 pending 移除 */
    public void ack(String... ids) {
        if (ids == null || ids.length == 0) {
            return;
        }
        RecordId[] recordIds = new RecordId[ids.length];
        for (int i = 0; i < ids.length; i++) {
            recordIds[i] = RecordId.of(ids[i]);
        }
        stringRedisTemplate.opsForStream().acknowledge(RedisKeys.seckillTaskStream(),
                Constants.SECKILL_TASK_GROUP, recordIds);
    }

    /** 保证 stream 与消费组存在（重复调用安全） */
    public void ensureGroup() {
        String key = RedisKeys.seckillTaskStream();
        String group = Constants.SECKILL_TASK_GROUP;
        try {
            // 原生 XGROUP ... MKSTREAM：一步建流 + 建组。
            // 不用 Spring Data 的 createGroup：它在 Redis 5.0 上会因 offset 处理差异报 NOGROUP。
            exec("XGROUP", "CREATE", key, group, "$", "MKSTREAM");
            log.info("秒杀任务队列就绪：stream={}, group={}", key, group);
        } catch (Exception e) {
            try {
                // 兜底：老版本 Redis 不认 MKSTREAM，先塞一条哨兵消息把流建出来再建组
                stringRedisTemplate.opsForStream().add(StreamRecords.newRecord()
                        .ofMap(Collections.singletonMap(FIELD_INIT, "1")).withStreamKey(key));
                exec("XGROUP", "CREATE", key, group, "$");
                log.info("秒杀任务队列就绪（兼容模式）：stream={}, group={}", key, group);
            } catch (Exception e2) {
                // BUSYGROUP：组已存在，属正常重复调用
                log.debug("创建消费组返回（BUSYGROUP 表示已存在）：msg={}", e2.getMessage());
            }
        }
    }

    /** 执行原生 Redis 命令（XGROUP / XCLAIM 等，规避各版本 API 差异） */
    private Object exec(String command, String... args) {
        byte[][] raw = new byte[args.length][];
        for (int i = 0; i < args.length; i++) {
            raw[i] = bytes(args[i]);
        }
        return stringRedisTemplate.execute((RedisCallback<Object>) connection ->
                connection.execute(command, raw));
    }

    /** 裁剪历史消息，避免 stream 无限膨胀 */
    public void trim() {
        try {
            stringRedisTemplate.opsForStream().trim(RedisKeys.seckillTaskStream(), maxLen);
        } catch (Exception e) {
            log.debug("裁剪任务流失败（不影响业务）：msg={}", e.getMessage());
        }
    }

    private List<TaskMessage> toMessages(List<MapRecord<String, Object, Object>> records, long deliveries) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<TaskMessage> result = new ArrayList<>(records.size());
        for (MapRecord<String, Object, Object> record : records) {
            result.add(new TaskMessage(record.getId().getValue(), read(record), deliveries));
        }
        return result;
    }

    private SeckillTaskBO read(MapRecord<String, Object, Object> record) {
        Object value = record.getValue().get(FIELD_TASK);
        return value == null ? null : readJson(String.valueOf(value));
    }

    private SeckillTaskBO readJson(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, SeckillTaskBO.class);
        } catch (Exception e) {
            log.error("任务反序列化失败（将确认并丢弃）：msg={}", e.getMessage());
            return null;
        }
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private String str(Object value) {
        if (value == null) {
            return "";
        }
        return value instanceof byte[] ? new String((byte[]) value, StandardCharsets.UTF_8) : String.valueOf(value);
    }

    private long toLong(Object value) {
        String text = str(value);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String write(SeckillTaskBO taskBO) {
        try {
            return objectMapper.writeValueAsString(taskBO);
        } catch (Exception e) {
            throw new IllegalStateException("秒杀任务序列化失败", e);
        }
    }

    /**
     * 队列里的一条任务：消息 ID + 任务体 + 已被投递的次数（含本次）。
     */
    @Data
    public static class TaskMessage {
        private final String id;
        private final SeckillTaskBO task;
        private final long deliveries;

        public TaskMessage(String id, SeckillTaskBO task, long deliveries) {
            this.id = id;
            this.task = task;
            this.deliveries = deliveries;
        }
    }
}

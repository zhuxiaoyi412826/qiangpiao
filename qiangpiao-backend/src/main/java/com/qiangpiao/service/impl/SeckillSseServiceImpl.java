package com.qiangpiao.service.impl;

import com.qiangpiao.service.SeckillSseService;
import com.qiangpiao.vo.SeckillPushVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SSE 推送实现：连接按「用户 + 批次」维度保存，异步落库出结果时主动推给前端。
 * <p>
 * 推送失败（连接已断、网络异常）只记日志，不影响抢票主流程 —— 前端会降级为轮询。
 */
@Slf4j
@Service
public class SeckillSseServiceImpl implements SeckillSseService {

    /** 连接最长存活时间（毫秒）：超时后前端会重连 */
    private static final long TIMEOUT = 5 * 60 * 1000L;

    private final ConcurrentMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Long userId, String batchNo) {
        String key = key(userId, batchNo);
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(key, emitter);
        Runnable cleanup = () -> emitters.remove(key);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());
        try {
            // 先发一条 connected，前端据此确认通道可用、停止高频轮询
            emitter.send(SseEmitter.event().name("connected").data("{\"batchNo\":\"" + batchNo + "\"}"));
        } catch (IOException e) {
            log.warn("SSE 连接建立后立即断开：userId={}, batchNo={}", userId, batchNo);
            emitters.remove(key);
        }
        log.info("SSE 订阅建立：userId={}, batchNo={}, 在线={}", userId, batchNo, emitters.size());
        return emitter;
    }

    @Override
    public void push(Long userId, String batchNo, SeckillPushVO vo) {
        String key = key(userId, batchNo);
        SseEmitter emitter = emitters.get(key);
        if (emitter == null) {
            log.debug("无 SSE 连接，跳过推送（前端走轮询）：userId={}, batchNo={}", userId, batchNo);
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("seckill-result").data(vo));
            log.info("SSE 推送成功：userId={}, batchNo={}, orderNo={}, status={}",
                    userId, batchNo, vo.getOrderNo(), vo.getStatus());
            if (Boolean.TRUE.equals(vo.getFinished())) {
                emitter.complete();
                emitters.remove(key);
            }
        } catch (Exception e) {
            log.warn("SSE 推送失败，前端将降级为轮询：userId={}, msg={}", userId, e.getMessage());
            emitters.remove(key);
        }
    }

    @Override
    public int onlineCount() {
        return emitters.size();
    }

    private String key(Long userId, String batchNo) {
        return userId + ":" + batchNo;
    }
}

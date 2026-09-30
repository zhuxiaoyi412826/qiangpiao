package com.qiangpiao.service;

import com.qiangpiao.vo.SeckillPushVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 抢票结果推送（SSE）：替代前端 1~2 秒轮询。
 * <pre>
 *   前端 EventSource 订阅 /api/seckill/stream?batchNo=xxx
 *   异步落库每出一张票的结果推一次
 *   连接断开 / 浏览器不支持 → 前端自动降级为轮询兜底
 * </pre>
 */
public interface SeckillSseService {

    /**
     * 订阅某个批次的抢票结果。
     *
     * @param userId  用户 ID（隔离不同用户的连接）
     * @param batchNo 批次号
     */
    SseEmitter subscribe(Long userId, String batchNo);

    /** 推送一张票的结果 */
    void push(Long userId, String batchNo, SeckillPushVO vo);

    /** 当前在线连接数（监控用） */
    int onlineCount();
}

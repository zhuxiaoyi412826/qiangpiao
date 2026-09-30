package com.qiangpiao.common.util;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 线程池 MDC 传递装饰器：把提交任务时的 traceId / userId / orderNo 带进异步线程，
 * 并在任务结束后清理，避免线程复用导致日志串号。
 * <p>
 * 秒杀下单跑在 seckillExecutor 上，没有它的话异步落库日志的 traceId 全是空，
 * 请求线程与异步线程对不上账，排障时只能靠时间戳猜。
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        final Map<String, String> callerContext = MDC.getCopyOfContextMap();
        final String callerTraceId = TraceContext.traceId();
        return () -> {
            try {
                if (callerContext != null) {
                    MDC.setContextMap(callerContext);
                } else if (callerTraceId != null) {
                    TraceContext.putTraceId(callerTraceId);
                }
                runnable.run();
            } finally {
                TraceContext.clear();
            }
        };
    }
}

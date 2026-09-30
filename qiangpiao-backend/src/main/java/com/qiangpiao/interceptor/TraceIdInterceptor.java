package com.qiangpiao.interceptor;

import com.qiangpiao.common.util.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 请求入口生成 traceId 并写入 MDC，串联一次请求内的全部业务日志（含线程池异步下单）。
 * 响应头回传 X-Trace-Id，便于前端 / 压测脚本与服务端日志对账。
 */
@Slf4j
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    public static final String TRACE_HEADER = "X-Trace-Id";

    /** 慢请求阈值（ms）：超过就打 warn，压测 / 线上排障一眼能看到卡在哪 */
    private static final long SLOW_THRESHOLD_MS = 1000L;

    private static final String START_TIME_ATTR = TraceIdInterceptor.class.getName() + ".startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = TraceContext.newTraceId();
        }
        TraceContext.putTraceId(traceId);
        response.setHeader(TRACE_HEADER, traceId);
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        log.info("请求开始 {} {} ip={}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Object startObj = request.getAttribute(START_TIME_ATTR);
        long cost = startObj instanceof Long ? System.currentTimeMillis() - (Long) startObj : -1L;
        String logMsg = "请求结束 {} {} status={} cost={}ms";
        if (cost >= SLOW_THRESHOLD_MS) {
            log.warn(logMsg, request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
        } else {
            log.info(logMsg, request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
        }
        // 拦截器是最内层入口，请求结束清理 MDC，避免容器线程复用导致 traceId 串号
        TraceContext.clear();
    }
}

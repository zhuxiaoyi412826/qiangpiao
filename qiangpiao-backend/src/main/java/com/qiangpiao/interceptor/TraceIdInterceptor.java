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

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = TraceContext.newTraceId();
        }
        TraceContext.putTraceId(traceId);
        response.setHeader(TRACE_HEADER, traceId);
        log.info("请求开始 {} {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        log.info("请求结束 {} {} status={}", request.getMethod(), request.getRequestURI(), response.getStatus());
        TraceContext.clear();
    }
}

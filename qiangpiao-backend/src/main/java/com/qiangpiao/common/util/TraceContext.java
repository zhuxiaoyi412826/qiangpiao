package com.qiangpiao.common.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * MDC 日志追踪上下文：把一次请求的 traceId、用户、订单号串进所有业务日志。
 * <pre>
 *   配置方式：TraceIdInterceptor 在请求入口生成 traceId 写入 MDC，
 *   业务链路（下单 / 支付 / 退票 / 改签）调用 putOrder 把订单号也放进 MDC，
 *   logback pattern 通过 %X{traceId} / %X{orderNo} 输出。
 * </pre>
 */
public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String ORDER_NO = "orderNo";

    private TraceContext() {
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static String traceId() {
        return MDC.get(TRACE_ID);
    }

    public static void putTraceId(String traceId) {
        MDC.put(TRACE_ID, traceId);
    }

    public static void putUser(Object userId) {
        MDC.put(USER_ID, userId == null ? "-" : String.valueOf(userId));
    }

    /** 订单链路专用：下单后调用，之后该订单所有日志都会带上订单号 */
    public static void putOrder(String orderNo) {
        MDC.put(ORDER_NO, orderNo == null ? "-" : orderNo);
    }

    public static void clear() {
        MDC.remove(TRACE_ID);
        MDC.remove(USER_ID);
        MDC.remove(ORDER_NO);
    }
}

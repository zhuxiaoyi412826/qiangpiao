package com.qiangpiao.common.result;

import com.qiangpiao.common.util.TraceContext;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 统一 REST 响应体（前后端分离出参规范）。
 *
 * @param <T> 业务数据类型
 */
@Data
@Accessors(chain = true)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务状态码 */
    private int code;
    /** 提示信息 */
    private String message;
    /** 业务数据 */
    private T data;
    /** 链路追踪 ID */
    private String traceId;
    /** 响应时间戳 */
    private long timestamp = System.currentTimeMillis();

    public static <T> R<T> ok() {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return build(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> ok(String message, T data) {
        return build(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> fail(String message) {
        return build(ResultCode.SYSTEM_ERROR.getCode(), message, null);
    }

    public static <T> R<T> fail(ResultCode resultCode) {
        return build(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return build(resultCode.getCode(), message, null);
    }

    public static <T> R<T> fail(int code, String message) {
        return build(code, message, null);
    }

    public static <T> R<T> build(int code, String message, T data) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        r.setData(data);
        // 回填链路追踪 ID：与响应头 X-Trace-Id、日志 %X{traceId} 三者一致，
        // 用户报障时只需给出这个 ID，就能从日志里捞出该请求的全部链路（含异步下单线程）。
        r.setTraceId(TraceContext.traceId());
        return r;
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}

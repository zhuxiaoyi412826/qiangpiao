package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单流转日志 DO（对应 t_order_log）：支撑订单详情的物流式时间轴与退改签历史。
 */
@Data
public class OrderLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    /** 动作：CREATE / PAY / CANCEL / REFUND / CHANGE / EXPIRE */
    private String action;
    private String actionText;
    private String detail;
    /** 操作人：用户ID 或 admin */
    private String operator;
    /** 日志追踪 ID（MDC） */
    private String traceId;
    private LocalDateTime createTime;
}

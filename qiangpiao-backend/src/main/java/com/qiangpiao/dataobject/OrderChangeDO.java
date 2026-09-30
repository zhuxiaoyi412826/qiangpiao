package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 改签记录 DO（对应 t_order_change）。
 */
@Data
public class OrderChangeDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private String newOrderNo;
    private Long userId;
    private Long oldTrainId;
    private Long newTrainId;
    private String oldSeatNo;
    private String newSeatNo;
    /** 差额：补款为正，退款为负 */
    private BigDecimal diffAmount;
    /**
     * 改签手续费：以新旧两张票里较低的票价为基数，按改签场景费率计收（见 ChangeFeeUtil）。
     * 与票价差额合并为净额：正数随补差价一并补收，负数从应退差额中扣除。
     */
    private BigDecimal changeFee;
    /** 本次改签的计费档位说明，便于对账与展示 */
    private String feeRule;
    private String reason;
    private LocalDateTime createTime;
}

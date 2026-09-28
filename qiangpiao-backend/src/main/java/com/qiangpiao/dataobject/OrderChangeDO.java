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
    private String reason;
    private LocalDateTime createTime;
}

package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 车票 DO：订单 + 车次联表结果（对应 t_order JOIN t_train）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TicketDO extends OrderDO {

    private static final long serialVersionUID = 1L;

    private String trainNo;
    private String trainType;
    private String fromStationName;
    private String toStationName;
    private LocalDate departDate;
    private LocalTime departTime;
    private LocalTime arriveTime;
    private Integer durationMinutes;
}

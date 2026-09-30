package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 车次 DO（对应 t_train）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private String trainNo;
    private String trainType;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private LocalDate departDate;
    private LocalTime departTime;
    private LocalTime arriveTime;
    private Integer durationMinutes;
    private Integer status;
    /** 售票开始时间：为空表示不限制（早于该时间点不可购买） */
    private java.time.LocalDateTime saleStartTime;
    /** 售票结束时间：为空表示不限制（晚于该时间点不可购买） */
    private java.time.LocalDateTime saleEndTime;
}

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
}

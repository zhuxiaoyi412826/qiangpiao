package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * 车次时刻表 DO（对应 t_train_stop）：每个途经站的到站 / 发车时刻。
 */
@Data
public class TrainStopDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long trainId;
    private Long stationId;
    private String stationName;
    /** 停靠顺序，从 1 开始 */
    private Integer stopOrder;
    private LocalTime arriveTime;
    private LocalTime departTime;
    private Integer stopMinutes;
    private Integer distanceKm;
}

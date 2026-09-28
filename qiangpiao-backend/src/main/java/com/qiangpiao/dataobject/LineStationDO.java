package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;

/**
 * 线路途经站 DO（对应 t_line_station）。
 */
@Data
public class LineStationDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long lineId;
    private Long stationId;
    private String stationName;
    /** 途经顺序，从 1 开始 */
    private Integer stopOrder;
}

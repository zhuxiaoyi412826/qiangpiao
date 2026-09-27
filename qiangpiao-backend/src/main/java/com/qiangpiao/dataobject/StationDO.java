package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 车站 DO（对应 t_station）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StationDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private String stationName;
    private String city;
    private String pyCode;
}

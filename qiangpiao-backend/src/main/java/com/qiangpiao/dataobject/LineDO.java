package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 线路 DO（对应 t_line）。
 */
@Data
public class LineDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String lineName;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    /** 1-启用 0-停用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

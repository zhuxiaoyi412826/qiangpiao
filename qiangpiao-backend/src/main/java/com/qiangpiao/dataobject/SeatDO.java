package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 座位 DO（对应 t_seat）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeatDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long trainId;
    private Integer seatType;
    private Integer carriageNo;
    private String seatNo;
    private Integer status;
    private String orderNo;
    private Integer version;
}

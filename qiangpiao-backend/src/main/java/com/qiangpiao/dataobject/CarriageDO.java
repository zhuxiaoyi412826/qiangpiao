package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 车次车厢 DO（对应 t_carriage）。
 */
@Data
public class CarriageDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long trainId;
    private Integer carriageNo;
    /** 席别：1-商务座 2-一等座 3-二等座 4-软卧 5-硬卧 */
    private Integer seatType;
    private Integer seatCount;
    private LocalDateTime createTime;
}

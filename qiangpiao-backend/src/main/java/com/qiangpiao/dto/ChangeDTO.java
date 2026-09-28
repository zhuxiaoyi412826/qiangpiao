package com.qiangpiao.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 改签入参：原订单换到新车次。
 */
@Data
public class ChangeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    /** 目标车次ID */
    private Long newTrainId;
    /** 目标席别：1-商务座 2-一等座 3-二等座 */
    private Integer seatType;
    private String reason;
}

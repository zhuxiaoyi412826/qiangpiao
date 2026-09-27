package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 座位 BO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long seatId;
    private Long trainId;
    private Integer seatType;
    private Integer carriageNo;
    private String seatNo;
    private Integer status;
}

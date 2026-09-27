package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单 BO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private Long trainId;
    private Long seatId;
    private Integer seatType;
    private Integer carriageNo;
    private String seatNo;
    private String passengerName;
    private String idCard;
    private BigDecimal price;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;

    /** 冗余：车次信息（跨服务组装） */
    private String trainNo;
    private String fromStationName;
    private String toStationName;
    private LocalDateTime departTime;
}

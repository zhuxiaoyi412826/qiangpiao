package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单 DO（对应 t_order）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private String orderNo;
    private Long userId;
    private Long trainId;
    /**
     * 车次快照：下单当时把车次号 / 站名 / 时刻写进订单。
     * 车次会按天滚动、也可能被重建清理，历史订单不能依赖 t_train 还在。
     */
    private String trainNoSnapshot;
    private String trainTypeSnapshot;
    private String fromStationSnapshot;
    private String toStationSnapshot;
    private java.time.LocalTime departTimeSnapshot;
    private java.time.LocalTime arriveTimeSnapshot;
    private Long seatId;
    private Integer seatType;
    private Integer carriageNo;
    private String seatNo;
    private String passengerName;
    private String idCard;
    private BigDecimal price;
    private Integer status;
    /** 乘车日期快照：车次日期会随跨天滚动变化，订单必须记录购票当时的发车日期 */
    private LocalDate departDate;
    private LocalDateTime payTime;
    private LocalDateTime cancelTime;
    private LocalDateTime expireTime;
}

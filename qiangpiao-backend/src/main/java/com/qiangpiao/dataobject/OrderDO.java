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
    /** 退票手续费：阶梯计费（开车前 8 天以上免收） */
    private BigDecimal refundFee;
    /** 实退金额 = 票价 - 手续费 */
    private BigDecimal refundAmount;
    /**
     * 最初购票车次的开车时间：改签后不更新。
     * 退票费率按此时间取档 —— 原票不足 8 天改签到 8 天以后再退，依然按 5% 收取。
     */
    private LocalDateTime originDepartTime;
    /** 是否改签过：0-否 1-是（开车后改签过的车票不可退票） */
    private Integer changed;
    /** 上车站序号（区间票：对应 t_train_stop.stop_order）；为空表示全程票 */
    private Integer fromStopOrder;
    /** 下车站序号（区间票）；为空表示全程票 */
    private Integer toStopOrder;
}

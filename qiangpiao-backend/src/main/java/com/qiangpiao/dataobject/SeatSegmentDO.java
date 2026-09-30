package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 座位区间占用 DO（对应 t_seat_segment）。
 * <p>
 * 区间语义 [fromOrder, toOrder)：乘客从第 fromOrder 站坐到第 toOrder 站，
 * 因此两个区间 [a,b) 与 [fo,to) 冲突 ⇔ a &lt; to &amp;&amp; fo &lt; b。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeatSegmentDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long trainId;
    private Long seatId;
    private Integer seatType;
    /** 上车站序号（含） */
    private Integer fromOrder;
    /** 下车站序号（不含） */
    private Integer toOrder;
    private String orderNo;
    /** 1-占用中 0-已释放 */
    private Integer status;
}

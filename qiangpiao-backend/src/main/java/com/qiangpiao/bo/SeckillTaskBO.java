package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀下单任务 BO：Redis 预扣库存成功后投递到线程池异步落库。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillTaskBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 预生成订单号 */
    private String orderNo;
    private Long userId;
    private Long trainId;
    private Integer seatType;
    /** 指定座位（可为空，为空则系统自动分配） */
    private Long seatId;
    /** 所属批次号（一次买多张时用于「优先同车厢」协调） */
    private String batchNo;
    /** 批次内第几张（从 1 开始），抢票流水按它排序展示 */
    private Integer ticketIndex;
    /** 受理时的排队序号，写进流水便于复盘 */
    private Long queueSeq;
    /** 优先分配的车厢号：同批次第一张票落位后写入 Redis，后续票尽量跟着坐一起 */
    private Integer preferCarriageNo;
    private String passengerName;
    private String idCard;
    private String clientIp;
    /** 乘车区间（区间票）：上车站序号 */
    private Integer fromStopOrder;
    /** 乘车区间（区间票）：下车站序号 */
    private Integer toStopOrder;
    /** 区间票价：未按区间定价时为 null，落库时仍用席别票价 */
    private java.math.BigDecimal segmentPrice;
}

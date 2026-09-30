package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀抢票流水 DO（对应 t_seckill_flow）：一次抢票请求从「受理」到「落库 / 失败」的完整留痕。
 * <p>
 * 与 t_order_log 的分工：
 * <ul>
 *   <li>t_order_log：订单维度的时间轴，从受理到退改签，给订单详情页看；</li>
 *   <li>t_seckill_flow：抢票请求维度的流水，带耗时与失败原因，给「我的抢票记录」和后台排查看。</li>
 * </ul>
 */
@Data
public class SeckillFlowDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String batchNo;
    private String orderNo;
    private Long userId;
    private Long trainId;
    private Integer seatType;
    private String passengerName;
    /** 批次内序号（第几张） */
    private Integer ticketIndex;
    /** 状态：0-排队中 1-抢票成功 2-抢票失败 */
    private Integer status;
    /** 失败原因（成功时为空） */
    private String failReason;
    /** 受理时的排队号 */
    private Long queueSeq;
    /** 受理到出结果的耗时（毫秒） */
    private Long costMs;
    private String clientIp;
    private LocalDateTime createTime;
    private LocalDateTime finishTime;
}

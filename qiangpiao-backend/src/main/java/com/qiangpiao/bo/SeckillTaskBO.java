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
    private String passengerName;
    private String idCard;
    private String clientIp;
}

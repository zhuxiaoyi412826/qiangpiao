package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 秒杀记录 DO（对应 t_seckill_record，一人一单幂等）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillRecordDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long trainId;
    private Integer seatType;
    private Long userId;
    private String orderNo;
}

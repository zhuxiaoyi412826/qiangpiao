package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 秒杀记录 DO（对应 t_seckill_record）。
 * <p>
 * 唯一键 (train_id, seat_type, user_id, id_card)：同一用户可为不同乘车人各买一张，
 * 但同一乘车人同一车次同一席别只能有一张（身份证为密文，确定性加密可等值比较）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeckillRecordDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long trainId;
    private Integer seatType;
    private Long userId;
    private String orderNo;
    /** 乘车人身份证（密文） */
    private String idCard;
}

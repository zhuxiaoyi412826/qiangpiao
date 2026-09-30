package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 常用乘车人 DO（对应 t_passenger）。
 * <p>
 * idCard 落库为密文（见 {@link com.qiangpiao.common.util.IdCardCrypto}）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PassengerDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String passengerName;
    /** 身份证号：库里存密文（ENC: 前缀 + Base64） */
    private String idCard;
    private String phone;
    /** 乘客类型：1-成人 2-儿童 3-学生 4-残军 */
    private Integer passengerType;
}

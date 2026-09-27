package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 钱包 DO（对应 t_wallet）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WalletDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long userId;
    /** 余额 */
    private BigDecimal balance;
    /** 累计充值 */
    private BigDecimal totalRecharge;
    /** 累计消费 */
    private BigDecimal totalConsume;
    private Integer version;
}

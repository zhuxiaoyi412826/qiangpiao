package com.qiangpiao.dataobject;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 零钱流水 DO（对应 t_wallet_flow）。
 * type：1-充值 2-消费 3-退款
 * amount：变动金额，消费为负、充值与退款为正
 * balance：变动之后的余额快照
 */
@Data
public class WalletFlowDO {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String flowNo;
    private Long userId;
    /** 业务单号：支付时为订单号，充值时为充值流水号 */
    private String bizNo;
    private Integer type;
    private String title;
    private String detail;
    private BigDecimal amount;
    private BigDecimal balance;
    private String remark;
    private LocalDateTime createTime;
}

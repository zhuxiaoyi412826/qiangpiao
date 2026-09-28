package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水 DO（对应 t_payment）：
 * 一次「发起支付」生成一笔支付单，渠道异步回调后落终态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    /** 支付流水号 */
    private String payNo;
    /** 订单号 */
    private String orderNo;
    /** 用户ID */
    private Long userId;
    /** 支付方式：ALIPAY / WECHAT / BALANCE */
    private String payType;
    /** 支付金额 */
    private BigDecimal amount;
    /** 支付状态：0-支付中 1-支付成功 2-支付失败 3-已关闭 */
    private Integer status;
    /** 幂等键：同一键只生成一笔有效支付单 */
    private String idempotentKey;
    /** 渠道交易号（回调带回） */
    private String tradeNo;
    /** 回调次数（含重复回调） */
    private Integer notifyCount;
    /** 支付成功时间 */
    private LocalDateTime payTime;
    /** 最近一次回调时间 */
    private LocalDateTime notifyTime;
    /** 支付单超时时间 */
    private LocalDateTime expireTime;
    /** 失败 / 关闭原因 */
    private String failReason;
}

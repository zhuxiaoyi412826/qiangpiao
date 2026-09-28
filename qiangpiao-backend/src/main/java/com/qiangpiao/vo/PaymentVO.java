package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付单 VO：发起支付后返回给前端，前端据此轮询支付结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("支付单")
public class PaymentVO {

    @ApiModelProperty("支付流水号")
    private String payNo;
    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("支付方式")
    private String payType;
    @ApiModelProperty("支付金额")
    private BigDecimal amount;
    @ApiModelProperty("支付状态：0-支付中 1-支付成功 2-支付失败 3-已关闭")
    private Integer status;
    @ApiModelProperty("支付状态文案")
    private String statusText;
    @ApiModelProperty("渠道交易号")
    private String tradeNo;
    @ApiModelProperty("回调次数")
    private Integer notifyCount;
    @ApiModelProperty("支付单超时时间")
    private LocalDateTime expireTime;
    @ApiModelProperty("支付成功时间")
    private LocalDateTime payTime;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    @ApiModelProperty("失败原因")
    private String failReason;
}

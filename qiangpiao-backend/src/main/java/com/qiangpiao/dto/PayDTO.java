package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 支付入参 DTO。
 */
@Data
@ApiModel("支付入参")
public class PayDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "订单号不能为空")
    @ApiModelProperty(value = "订单号", required = true)
    private String orderNo;

    @ApiModelProperty(value = "支付方式：ALIPAY / WECHAT / BALANCE", example = "ALIPAY")
    private String payType = "ALIPAY";
}

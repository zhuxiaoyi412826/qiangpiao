package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包充值入参 DTO（自定义充值金额）。
 */
@Data
@ApiModel("钱包充值入参")
public class RechargeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "充值金额不能为空")
    @DecimalMin(value = "0.01", message = "充值金额必须大于 0")
    @Digits(integer = 8, fraction = 2, message = "充值金额最多保留 2 位小数")
    @ApiModelProperty(value = "充值金额（自定义，最多 2 位小数）", required = true, example = "100.00")
    private BigDecimal amount;

    @ApiModelProperty(value = "备注", example = "模拟充值")
    private String remark;
}

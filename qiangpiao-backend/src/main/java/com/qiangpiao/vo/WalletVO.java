package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 钱包出参 VO。
 */
@Data
@Builder
@ApiModel("钱包信息")
public class WalletVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("用户ID")
    private Long userId;

    @ApiModelProperty("余额")
    private BigDecimal balance;

    @ApiModelProperty("累计充值")
    private BigDecimal totalRecharge;

    @ApiModelProperty("累计消费")
    private BigDecimal totalConsume;
}

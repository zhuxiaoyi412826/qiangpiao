package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 零钱流水出参 VO。
 */
@Data
@Builder
@ApiModel("零钱流水")
public class WalletFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("流水ID")
    private Long id;

    @ApiModelProperty("流水号")
    private String flowNo;

    @ApiModelProperty("业务单号（订单号 / 充值流水号）")
    private String bizNo;

    @ApiModelProperty("类型：1-充值 2-消费 3-退款")
    private Integer type;

    @ApiModelProperty("类型文案")
    private String typeText;

    @ApiModelProperty("摘要，如：购买 G1001 二等座")
    private String title;

    @ApiModelProperty("明细，如：北京南 -> 上海虹桥 05车12A")
    private String detail;

    @ApiModelProperty("变动金额：消费为负，充值/退款为正")
    private BigDecimal amount;

    @ApiModelProperty("变动后余额")
    private BigDecimal balance;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("发生时间")
    private LocalDateTime createTime;
}

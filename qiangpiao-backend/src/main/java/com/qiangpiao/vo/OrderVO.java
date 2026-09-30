package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单出参")
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID")
    private Long id;
    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("车次号")
    private String trainNo;
    @ApiModelProperty("出发站")
    private String fromStationName;
    @ApiModelProperty("到达站")
    private String toStationName;
    @ApiModelProperty("出发时间")
    private LocalDateTime departTime;
    @ApiModelProperty("席别")
    private Integer seatType;
    @ApiModelProperty("席别名称")
    private String seatTypeName;
    @ApiModelProperty("车厢号")
    private Integer carriageNo;
    @ApiModelProperty("座位号")
    private String seatNo;
    @ApiModelProperty("乘客姓名")
    private String passengerName;
    @ApiModelProperty("票价")
    private BigDecimal price;
    @ApiModelProperty("订单状态：0-待支付 1-已支付 2-已取消 3-已退票 4-已超时")
    private Integer status;
    @ApiModelProperty("订单状态描述")
    private String statusText;
    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;
    @ApiModelProperty("支付截止时间")
    private LocalDateTime expireTime;
    @ApiModelProperty("退票手续费（已退票时有效）")
    private BigDecimal refundFee;
    @ApiModelProperty("实退金额（已退票时有效）")
    private BigDecimal refundAmount;
    @ApiModelProperty("是否改签过：0-否 1-是")
    private Integer changed;
}

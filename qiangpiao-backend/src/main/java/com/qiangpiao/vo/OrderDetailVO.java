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
 * 订单详情出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("订单详情出参")
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("车次号")
    private String trainNo;
    @ApiModelProperty("出发站")
    private String fromStationName;
    @ApiModelProperty("到达站")
    private String toStationName;
    @ApiModelProperty("出发日期")
    private String departDate;
    @ApiModelProperty("出发时间")
    private String departTime;
    @ApiModelProperty("到达时间")
    private String arriveTime;
    @ApiModelProperty("席别名称")
    private String seatTypeName;
    @ApiModelProperty("车厢号")
    private Integer carriageNo;
    @ApiModelProperty("座位号")
    private String seatNo;
    @ApiModelProperty("乘客姓名")
    private String passengerName;
    @ApiModelProperty("身份证号（脱敏）")
    private String idCard;
    @ApiModelProperty("票价")
    private BigDecimal price;
    @ApiModelProperty("订单状态")
    private Integer status;
    @ApiModelProperty("订单状态描述")
    private String statusText;
    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;
    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;
    @ApiModelProperty("支付截止时间")
    private LocalDateTime expireTime;
}

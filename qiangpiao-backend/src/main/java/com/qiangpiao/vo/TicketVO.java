package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的车票出参 VO。
 */
@Data
@Builder
@ApiModel("车票出参")
public class TicketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单ID（车票ID）")
    private Long id;

    @ApiModelProperty("订单号")
    private String orderNo;

    @ApiModelProperty("车次号")
    private String trainNo;

    @ApiModelProperty("车次类型：高铁 / 动车")
    private String trainType;

    @ApiModelProperty("出发站")
    private String fromStationName;

    @ApiModelProperty("到达站")
    private String toStationName;

    @ApiModelProperty("乘车日期")
    private String departDate;

    @ApiModelProperty("发车时刻")
    private String departTime;

    @ApiModelProperty("到达时刻")
    private String arriveTime;

    @ApiModelProperty("历时")
    private String durationText;

    @ApiModelProperty("席别名称")
    private String seatTypeName;

    @ApiModelProperty("车厢号")
    private Integer carriageNo;

    @ApiModelProperty("座位号")
    private String seatNo;

    @ApiModelProperty("乘车人")
    private String passengerName;

    @ApiModelProperty("票价")
    private BigDecimal price;

    @ApiModelProperty("订单状态：0-待支付 1-已支付 2-已取消 3-已退票 4-已超时")
    private Integer status;

    @ApiModelProperty("订单状态描述")
    private String statusText;

    @ApiModelProperty("车票状态：1-待出行 2-已出行 3-已失效")
    private Integer ticketStatus;

    @ApiModelProperty("车票状态描述")
    private String ticketStatusText;

    @ApiModelProperty("是否已发车")
    private Boolean departed;

    @ApiModelProperty("距今发车天数（待出行为正数，已开车为负数）")
    private Long daysFromNow;

    @ApiModelProperty("支付时间")
    private LocalDateTime payTime;

    @ApiModelProperty("下单时间")
    private LocalDateTime createTime;
}

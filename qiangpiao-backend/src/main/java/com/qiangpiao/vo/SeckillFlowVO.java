package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 抢票流水出参：一次抢票请求的受理与最终结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("抢票流水")
public class SeckillFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排队中 */
    public static final int STATUS_QUEUEING = 0;
    /** 抢票成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 抢票失败 */
    public static final int STATUS_FAILED = 2;

    @ApiModelProperty("流水ID")
    private Long id;
    @ApiModelProperty("批次号")
    private String batchNo;
    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("用户ID")
    private Long userId;
    @ApiModelProperty("车次ID")
    private Long trainId;
    @ApiModelProperty("席别")
    private Integer seatType;
    @ApiModelProperty("乘客姓名")
    private String passengerName;
    @ApiModelProperty("批次内第几张")
    private Integer ticketIndex;
    @ApiModelProperty("状态：0-排队中 1-成功 2-失败")
    private Integer status;
    @ApiModelProperty("状态文案")
    private String statusText;
    @ApiModelProperty("失败原因")
    private String failReason;
    @ApiModelProperty("受理时的排队序号")
    private Long queueSeq;
    @ApiModelProperty("耗时（毫秒）")
    private Long costMs;
    @ApiModelProperty("受理时间")
    private LocalDateTime createTime;
    @ApiModelProperty("出结果时间")
    private LocalDateTime finishTime;
}

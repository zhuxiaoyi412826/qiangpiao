package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 秒杀抢票出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("秒杀抢票出参")
public class SeckillResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 排队中 */
    public static final String STATUS_QUEUEING = "QUEUEING";
    /** 抢票成功 */
    public static final String STATUS_SUCCESS = "SUCCESS";
    /** 抢票失败 */
    public static final String STATUS_FAILED = "FAILED";

    @ApiModelProperty("订单号（排队中/成功时返回）")
    private String orderNo;
    @ApiModelProperty("状态：QUEUEING-排队中 SUCCESS-成功 FAILED-失败")
    private String status;
    @ApiModelProperty("提示信息")
    private String message;
    @ApiModelProperty("车次ID")
    private Long trainId;
    @ApiModelProperty("席别")
    private Integer seatType;
}

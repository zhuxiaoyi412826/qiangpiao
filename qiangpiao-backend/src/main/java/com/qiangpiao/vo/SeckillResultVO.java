package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

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

    @ApiModelProperty("订单号（排队中/成功时返回；批量时返回批次内第一张）")
    private String orderNo;
    @ApiModelProperty("批次号（一次买多张时用这个轮询 /api/seckill/batch/result）")
    private String batchNo;
    @ApiModelProperty("本次购票张数")
    private Integer count;
    @ApiModelProperty("本批次全部订单号")
    private List<String> orderNos;
    @ApiModelProperty("状态：QUEUEING-排队中 SUCCESS-成功 FAILED-失败")
    private String status;
    @ApiModelProperty("提示信息")
    private String message;
    @ApiModelProperty("车次ID")
    private Long trainId;
    @ApiModelProperty("席别")
    private Integer seatType;
    @ApiModelProperty("排队序号：本次请求在该车次席别队列中的受理次序")
    private Long queueSeq;
    @ApiModelProperty("前面还有多少张票在排队处理（0 表示马上轮到）")
    private Long queueAhead;
}

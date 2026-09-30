package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 抢票结果推送（SSE）。
 * <p>
 * 异步落库每出一张票的结果就推一次，前端收到后拉一次批次结果即可刷新整批状态；
 * SSE 不可用时前端降级为轮询，功能不受影响。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("抢票结果推送")
public class SeckillPushVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("批次号")
    private String batchNo;
    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("状态：0-排队中 1-成功 -1-失败")
    private Integer status;
    @ApiModelProperty("车厢号")
    private Integer carriageNo;
    @ApiModelProperty("座位号")
    private String seatNo;
    @ApiModelProperty("提示信息")
    private String message;
    /** 是否已全部出结果（前端据此关闭连接） */
    @ApiModelProperty("本批次是否已全部出结果")
    private Boolean finished;
}

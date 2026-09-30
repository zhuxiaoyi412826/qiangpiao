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
 * 批量抢票结果出参（一次买多张：每乘客一单，逐张给结果）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("批量抢票结果")
public class SeckillBatchResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 全部排队中 */
    public static final int STATUS_QUEUEING = 0;
    /** 全部成功 */
    public static final int STATUS_SUCCESS = 1;
    /** 部分成功（有的成功有的失败） */
    public static final int STATUS_PARTIAL = 2;
    /** 全部失败 */
    public static final int STATUS_FAILED = -1;

    @ApiModelProperty("批次号")
    private String batchNo;
    @ApiModelProperty("票数量")
    private int count;
    @ApiModelProperty("状态：0-排队中 1-全部成功 2-部分成功 -1-全部失败")
    private Integer status;
    @ApiModelProperty("整体提示")
    private String message;
    @ApiModelProperty("逐张票结果")
    private List<TicketResult> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @ApiModel("单张票结果")
    public static class TicketResult implements Serializable {

        private static final long serialVersionUID = 1L;

        @ApiModelProperty("订单号")
        private String orderNo;
        @ApiModelProperty("乘客姓名")
        private String passengerName;
        @ApiModelProperty("状态：0-排队中 1-成功 -1-失败")
        private Integer status;
        @ApiModelProperty("车厢号")
        private Integer carriageNo;
        @ApiModelProperty("座位号")
        private String seatNo;
        @ApiModelProperty("提示（失败原因）")
        private String message;
    }
}

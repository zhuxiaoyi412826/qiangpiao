package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 抢票结果轮询出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("抢票结果出参")
public class SeckillStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("订单号")
    private String orderNo;
    @ApiModelProperty("状态：-1-失败 0-排队中 1-抢票成功")
    private Integer status;
    @ApiModelProperty("车厢号")
    private Integer carriageNo;
    @ApiModelProperty("座位号")
    private String seatNo;
    @ApiModelProperty("提示信息")
    private String message;
}

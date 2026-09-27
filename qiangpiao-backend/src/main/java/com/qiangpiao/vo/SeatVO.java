package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 座位出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("座位出参")
public class SeatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("座位ID")
    private Long seatId;
    @ApiModelProperty("车厢号")
    private Integer carriageNo;
    @ApiModelProperty("座位号")
    private String seatNo;
    @ApiModelProperty("席别")
    private Integer seatType;
    @ApiModelProperty("状态：0-可选 1-已售 2-锁定")
    private Integer status;
}

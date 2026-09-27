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
 * 座位图出参 VO（按车厢分组展示，供前端选座）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("座位图出参")
public class SeatMapVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("车次ID")
    private Long trainId;
    @ApiModelProperty("席别")
    private Integer seatType;
    @ApiModelProperty("总座位")
    private Integer totalCount;
    @ApiModelProperty("剩余可选")
    private Integer availableCount;
    @ApiModelProperty("座位列表")
    private List<SeatVO> seats;
}

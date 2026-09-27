package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 车次详情出参 VO（含座位图）。
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ApiModel("车次详情出参")
public class TrainDetailVO extends TrainVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("座位图（按席别分组）")
    private List<SeatMapVO> seatMaps;
}

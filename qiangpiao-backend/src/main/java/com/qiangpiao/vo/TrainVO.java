package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 车次列表出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("车次出参")
public class TrainVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("车次ID")
    private Long id;
    @ApiModelProperty("车次号")
    private String trainNo;
    @ApiModelProperty("车型")
    private String trainType;
    @ApiModelProperty("出发站")
    private String fromStationName;
    @ApiModelProperty("到达站")
    private String toStationName;
    @ApiModelProperty("出发日期")
    private LocalDate departDate;
    @ApiModelProperty("出发时间")
    private LocalTime departTime;
    @ApiModelProperty("到达时间")
    private LocalTime arriveTime;
    @ApiModelProperty("历时")
    private String durationText;
    @ApiModelProperty("状态：1-可售 0-停运")
    private Integer status;
    @ApiModelProperty("各席别余票")
    private List<TrainStockVO> stocks;
    @ApiModelProperty("是否可购票：false 表示已发车 / 已停售 / 未开预售")
    private Boolean sellable;
    @ApiModelProperty("不可购票原因，为空表示可购买")
    private String sellTip;
}

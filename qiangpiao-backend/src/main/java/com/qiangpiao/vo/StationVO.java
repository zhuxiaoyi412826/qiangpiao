package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 车站出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("车站出参")
public class StationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("车站ID")
    private Long id;
    @ApiModelProperty("车站名称")
    private String stationName;
    @ApiModelProperty("城市")
    private String city;
    @ApiModelProperty("拼音简码")
    private String pyCode;
}

package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * 车次查询入参 DTO。
 */
@Data
@ApiModel("车次查询入参")
public class TrainQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "出发站", example = "北京南")
    private String fromStation;

    @ApiModelProperty(value = "到达站", example = "上海虹桥")
    private String toStation;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "出发日期 yyyy-MM-dd；按车次号查询时可留空，返回该车次全部未来班次")
    private LocalDate departDate;

    @ApiModelProperty(value = "车次号（如 G180），填写后优先按车次号查询，忽略出发 / 到达站", example = "G180")
    private String trainNo;

    @Min(value = 1, message = "页码不能小于 1")
    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;
}

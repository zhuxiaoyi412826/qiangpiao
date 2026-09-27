package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 我的车票查询入参 DTO。
 */
@Data
@ApiModel("我的车票查询入参")
public class TicketQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "车票类型：upcoming-未开车（默认） history-历史", example = "upcoming")
    private String type;

    @Min(value = 1, message = "页码不能小于 1")
    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 50, message = "每页条数不能超过 50")
    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;

    /**
     * true-历史车票（已开车 + 已取消/退票/超时），false-未开车
     */
    public boolean isHistory() {
        return "history".equalsIgnoreCase(type);
    }
}

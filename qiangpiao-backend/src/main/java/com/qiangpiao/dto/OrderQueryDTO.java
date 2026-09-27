package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * 订单分页查询入参 DTO。
 */
@Data
@ApiModel("订单查询入参")
public class OrderQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单状态：0-待支付 1-已支付 2-已取消 3-已退票 4-已超时（不传查全部）")
    private Integer status;

    @Min(value = 1, message = "页码不能小于 1")
    @ApiModelProperty(value = "页码", example = "1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    @ApiModelProperty(value = "每页条数", example = "10")
    private Integer pageSize = 10;
}

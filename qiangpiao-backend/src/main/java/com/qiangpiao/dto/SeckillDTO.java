package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 秒杀抢票入参 DTO。
 */
@Data
@ApiModel("秒杀抢票入参")
public class SeckillDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "车次 ID 不能为空")
    @ApiModelProperty(value = "车次ID", required = true, example = "1")
    private Long trainId;

    @NotNull(message = "席别不能为空")
    @Min(value = 1, message = "席别不合法")
    @Max(value = 3, message = "席别不合法")
    @ApiModelProperty(value = "席别：1-商务座 2-一等座 3-二等座", required = true, example = "3")
    private Integer seatType;

    @ApiModelProperty(value = "指定座位ID（不传则由系统自动分配）")
    private Long seatId;

    @NotBlank(message = "乘客姓名不能为空")
    @ApiModelProperty(value = "乘客姓名", required = true, example = "张三")
    private String passengerName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^(\\d{17}[0-9Xx]|\\d{15})$", message = "身份证号格式不正确")
    @ApiModelProperty(value = "身份证号", required = true)
    private String idCard;
}

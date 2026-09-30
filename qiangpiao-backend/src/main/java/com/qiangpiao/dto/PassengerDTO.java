package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 常用乘车人入参 DTO（身份证明文传入，落库加密）。
 */
@Data
@ApiModel("常用乘车人入参")
public class PassengerDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "乘客姓名不能为空")
    @ApiModelProperty(value = "乘客姓名", required = true, example = "张三")
    private String passengerName;

    @NotBlank(message = "身份证号不能为空")
    @Pattern(regexp = "^(\\d{17}[0-9Xx]|\\d{15})$", message = "身份证号格式不正确")
    @ApiModelProperty(value = "身份证号（明文传入，服务端加密落库）", required = true)
    private String idCard;

    @Pattern(regexp = "^(1[3-9]\\d{9})?$", message = "手机号格式不正确")
    @ApiModelProperty(value = "手机号", example = "13800138000")
    private String phone;

    @ApiModelProperty(value = "乘客类型：1-成人 2-儿童 3-学生 4-残军", example = "1")
    private Integer passengerType;
}

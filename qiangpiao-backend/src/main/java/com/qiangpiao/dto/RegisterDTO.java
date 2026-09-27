package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 注册入参 DTO。
 */
@Data
@ApiModel("注册入参")
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度必须在 2~32 之间")
    @ApiModelProperty(value = "用户名", required = true)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6~32 之间")
    @ApiModelProperty(value = "密码", required = true)
    private String password;

    @NotBlank(message = "真实姓名不能为空")
    @ApiModelProperty(value = "真实姓名", required = true)
    private String realName;

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @ApiModelProperty(value = "手机号", required = true)
    private String phone;

    @Pattern(regexp = "^(\\d{17}[0-9Xx]|\\d{15})$", message = "身份证号格式不正确")
    @ApiModelProperty(value = "身份证号")
    private String idCard;
}

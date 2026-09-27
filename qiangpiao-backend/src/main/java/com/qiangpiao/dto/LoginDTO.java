package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 登录入参 DTO。
 */
@Data
@ApiModel("登录入参")
public class LoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 32, message = "用户名长度必须在 2~32 之间")
    @ApiModelProperty(value = "用户名", required = true, example = "zhangsan")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度必须在 6~32 之间")
    @ApiModelProperty(value = "密码", required = true, example = "123456")
    private String password;
}

package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 人机验证入参：图形验证码与滑块二选一。
 */
@Data
@ApiModel("人机验证入参")
public class CaptchaVerifyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("图形验证码 ID")
    private String captchaId;
    @ApiModelProperty("图形验证码文本")
    private String captchaCode;
    @ApiModelProperty("滑块 ID")
    private String sliderId;
    @ApiModelProperty("滑块拖动到的 x 坐标（px）")
    private Integer sliderX;
}

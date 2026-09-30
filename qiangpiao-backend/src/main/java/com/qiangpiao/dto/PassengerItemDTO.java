package com.qiangpiao.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * 批量购票时的单个乘客入参。
 * <p>
 * 传 passengerId（常用乘车人）时无需再传证件号；否则需传 passengerName + idCard 明文。
 */
@Data
@ApiModel("购票乘客")
public class PassengerItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "常用乘车人ID（传了则无需姓名与身份证号）")
    private Long passengerId;

    @ApiModelProperty(value = "乘客姓名；不传 passengerId 时必填", example = "张三")
    private String passengerName;

    @Pattern(regexp = "^(\\d{17}[0-9Xx]|\\d{15})$", message = "身份证号格式不正确")
    @ApiModelProperty(value = "身份证号（明文）；不传 passengerId 时必填")
    private String idCard;

    @ApiModelProperty(value = "手机号（明文，选填；服务端做格式校验，仅本次购票使用，不落库）")
    private String phone;

    @ApiModelProperty(value = "乘客类型：1-成人 2-儿童 3-学生 4-残军")
    private Integer passengerType;
}

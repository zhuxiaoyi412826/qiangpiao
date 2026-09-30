package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 常用乘车人 VO：身份证对外只给脱敏值，下单时前端只回传 id，由后端取明文。
 */
@Data
@Builder
@ApiModel("常用乘车人")
public class PassengerVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("主键")
    private Long id;

    @ApiModelProperty("乘客姓名")
    private String passengerName;

    @ApiModelProperty("身份证号（脱敏，前4后4）")
    private String idCardMasked;

    @ApiModelProperty("手机号（脱敏）")
    private String phoneMasked;

    @ApiModelProperty("乘客类型：1-成人 2-儿童 3-学生 4-残军")
    private Integer passengerType;

    @ApiModelProperty("乘客类型文本")
    private String passengerTypeText;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}

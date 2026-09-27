package com.qiangpiao.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 席别库存出参 VO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("席别库存出参")
public class TrainStockVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("席别：1-商务座 2-一等座 3-二等座")
    private Integer seatType;
    @ApiModelProperty("席别名称")
    private String seatTypeName;
    @ApiModelProperty("票价")
    private BigDecimal price;
    @ApiModelProperty("剩余票数")
    private Integer availableCount;
    @ApiModelProperty("总票数")
    private Integer totalCount;
}

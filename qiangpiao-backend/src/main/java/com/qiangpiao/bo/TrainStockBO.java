package com.qiangpiao.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 席别库存 BO（Service 之间传递的业务对象）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("席别库存业务对象")
public class TrainStockBO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("库存ID")
    private Long id;
    @ApiModelProperty("车次ID")
    private Long trainId;
    @ApiModelProperty("席别")
    private Integer seatType;
    @ApiModelProperty("席别名称")
    private String seatTypeName;
    @ApiModelProperty("总座位")
    private Integer totalCount;
    @ApiModelProperty("剩余可售")
    private Integer availableCount;
    @ApiModelProperty("票价")
    private BigDecimal price;
    @ApiModelProperty("乐观锁版本")
    private Integer version;
}

package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 车次席别库存 DO（对应 t_train_stock，秒杀库存 + 乐观锁）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainStockDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long trainId;
    private Integer seatType;
    private Integer totalCount;
    private Integer availableCount;
    private BigDecimal price;
    private Integer version;
}

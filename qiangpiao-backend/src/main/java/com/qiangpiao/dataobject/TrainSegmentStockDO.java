package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 区间库存 DO（对应 t_train_segment_stock）。
 * <p>
 * segIndex = i 表示「第 i 站 → 第 i+1 站」这一段；
 * 任意 OD 区间（fo → to）的余票 = min(覆盖段 segIndex ∈ [fo, to) 的 availableCount)。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainSegmentStockDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private Long trainId;
    private Integer seatType;
    private Integer segIndex;
    private Integer totalCount;
    private Integer availableCount;
    private Integer version;
}

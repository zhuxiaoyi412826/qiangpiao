package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 乘车区间：把「上车站 → 下车站」翻译成经停站序号区间 [fromOrder, toOrder)。
 * <p>
 * 车次没有时刻表数据时退化为全程票 [1, 2)，行为与改造前一致。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RangeBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 全程票兜底：只有始发 / 终到两站 */
    public static final int DEFAULT_FROM = 1;
    public static final int DEFAULT_TO = 2;

    private Integer fromOrder;
    private Integer toOrder;
    private String fromStationName;
    private String toStationName;

    /** 是否为全程票（未指定区间或车次无时刻表） */
    public boolean isFullRange() {
        return fromOrder == null || toOrder == null || (fromOrder == DEFAULT_FROM && toOrder == DEFAULT_TO);
    }

    /** 覆盖的单段数量 = toOrder - fromOrder */
    public int segmentCount() {
        if (fromOrder == null || toOrder == null) {
            return 1;
        }
        return Math.max(toOrder - fromOrder, 1);
    }
}

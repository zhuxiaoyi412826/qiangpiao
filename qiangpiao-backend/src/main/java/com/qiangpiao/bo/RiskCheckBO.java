package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 风控检查结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskCheckBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否命中风控 */
    private boolean hit;
    /** 命中的具体原因（可能多条） */
    @Builder.Default
    private List<String> reasons = new ArrayList<>();
    /** 该维度累计命中次数（含本次） */
    private long hits;
    /** 本次是否被自动拉黑 */
    private boolean blocked;
}

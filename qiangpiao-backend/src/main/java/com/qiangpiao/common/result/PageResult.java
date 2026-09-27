package com.qiangpiao.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页出参封装。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码 */
    private long pageNum;
    /** 每页条数 */
    private long pageSize;
    /** 总条数 */
    private long total;
    /** 总页数 */
    private long pages;
    /** 数据列表 */
    private List<T> list = Collections.emptyList();

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> list) {
        long pages = pageSize <= 0 ? 0 : (total + pageSize - 1) / pageSize;
        return new PageResult<>(pageNum, pageSize, total, pages, list);
    }
}

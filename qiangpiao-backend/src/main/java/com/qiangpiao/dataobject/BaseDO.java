package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DO 基类：数据库实体公共字段。
 */
@Data
public class BaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

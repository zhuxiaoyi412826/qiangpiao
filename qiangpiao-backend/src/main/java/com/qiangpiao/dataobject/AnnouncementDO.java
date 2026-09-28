package com.qiangpiao.dataobject;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公告 DO（对应 t_announcement）：停运通知 / 节假日售票通知。
 */
@Data
public class AnnouncementDO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String content;
    /** 1-停运通知 2-节假日通知 3-其他 */
    private Integer type;
    /** 1-已发布 0-已下架 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String typeText() {
        if (type == null) {
            return "其他";
        }
        switch (type) {
            case 1:
                return "停运通知";
            case 2:
                return "节假日通知";
            default:
                return "其他";
        }
    }
}

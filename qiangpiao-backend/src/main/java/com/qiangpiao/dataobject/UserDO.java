package com.qiangpiao.dataobject;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户 DO（对应 t_user）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserDO extends BaseDO {

    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String realName;
    private String phone;
    private String idCard;
    private String role;
    private Integer status;
}

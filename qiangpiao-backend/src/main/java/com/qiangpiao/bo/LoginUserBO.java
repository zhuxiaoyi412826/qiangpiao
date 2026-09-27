package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 登录用户 BO（Service 层返回给 Controller 组装 VO）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private List<String> roles;
}

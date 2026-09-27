package com.qiangpiao.common.util;

import com.qiangpiao.common.context.UserContext;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 登录信息工具：Controller 层统一获取当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 当前登录用户 ID，未登录抛 401
     */
    public static Long currentUserId() {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return userId;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return ((LoginUser) authentication.getPrincipal()).getUserId();
        }
        throw new BizException(ResultCode.UNAUTHORIZED);
    }

    public static String currentUsername() {
        String username = UserContext.getUsername();
        if (username != null) {
            return username;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return ((LoginUser) authentication.getPrincipal()).getUsername();
        }
        throw new BizException(ResultCode.UNAUTHORIZED);
    }

    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}

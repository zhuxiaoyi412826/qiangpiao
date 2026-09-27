package com.qiangpiao.common.context;

import lombok.Data;

/**
 * 登录用户上下文（ThreadLocal），Service 层可直接获取当前登录人，无需层层传参。
 */
public final class UserContext {

    private static final ThreadLocal<LoginUserHolder> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(LoginUserHolder holder) {
        HOLDER.set(holder);
    }

    public static LoginUserHolder get() {
        return HOLDER.get();
    }

    public static Long getUserId() {
        LoginUserHolder holder = HOLDER.get();
        return holder == null ? null : holder.getUserId();
    }

    public static String getUsername() {
        LoginUserHolder holder = HOLDER.get();
        return holder == null ? null : holder.getUsername();
    }

    public static void clear() {
        HOLDER.remove();
    }

    @Data
    public static class LoginUserHolder {
        private Long userId;
        private String username;
        private String ip;

        public LoginUserHolder() {
        }

        public LoginUserHolder(Long userId, String username, String ip) {
            this.userId = userId;
            this.username = username;
            this.ip = ip;
        }
    }
}

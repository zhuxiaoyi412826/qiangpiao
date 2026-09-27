package com.qiangpiao.common.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 获取客户端真实 IP。
 */
public final class IpUtils {

    private IpUtils() {
    }

    private static final String[] HEADERS = {
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "X-Real-IP"
    };

    public static String getIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        for (String header : HEADERS) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}

package com.qiangpiao.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 支付回调验签工具（与第三方支付渠道同款约定）：
 * <pre>
 *   1. 参与签名的参数：除 sign 本身外的所有非空参数
 *   2. 按参数名 ASCII 升序排序，拼成 key1=value1&amp;key2=value2...
 *   3. 末尾追加 &amp;key={secret}
 *   4. 对拼接串做 HmacSHA256，转 16 进制大写即为 sign
 * </pre>
 * 校验时用 {@link MessageDigest#isEqual} 做定长比较，避免计时攻击。
 */
public final class PaymentSignUtil {

    /** 签名参数名 */
    public static final String SIGN_KEY = "sign";
    /** 密钥参数名（只参与拼接，不作为参数传递） */
    public static final String SECRET_KEY = "key";

    private static final String HMAC_SHA256 = "HmacSHA256";

    private PaymentSignUtil() {
    }

    /**
     * 构造待签名串：k=v 升序拼接 + &amp;key=secret。
     */
    public static String buildSignContent(Map<String, Object> params, String secret) {
        Map<String, Object> sorted = new TreeMap<>();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (key == null || SIGN_KEY.equals(key) || value == null) {
                    continue;
                }
                String text = String.valueOf(value);
                if (text.isEmpty()) {
                    continue;
                }
                sorted.put(key, text);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : sorted.entrySet()) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        if (sb.length() > 0) {
            sb.append('&');
        }
        sb.append(SECRET_KEY).append('=').append(secret == null ? "" : secret);
        return sb.toString();
    }

    /**
     * 生成签名（大写 16 进制）。
     */
    public static String sign(Map<String, Object> params, String secret) {
        return hmacSha256(buildSignContent(params, secret), secret);
    }

    /**
     * 校验回调签名。
     *
     * @return true-签名正确
     */
    public static boolean verify(Map<String, Object> params, String secret) {
        Object sign = params == null ? null : params.get(SIGN_KEY);
        if (sign == null) {
            return false;
        }
        String expected = sign(params, secret);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = String.valueOf(sign).toUpperCase().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    /**
     * 校验签名（参数是字符串 Map 的场景）。
     */
    public static boolean verify(Map<String, Object> params, String secret, String sign) {
        String expected = sign(params, secret);
        byte[] a = expected.getBytes(StandardCharsets.UTF_8);
        byte[] b = (sign == null ? "" : sign.toUpperCase()).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private static String hmacSha256(String content, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec((secret == null ? "" : secret).getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] bytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString().toUpperCase();
        } catch (Exception e) {
            throw new IllegalStateException("计算支付签名失败", e);
        }
    }
}

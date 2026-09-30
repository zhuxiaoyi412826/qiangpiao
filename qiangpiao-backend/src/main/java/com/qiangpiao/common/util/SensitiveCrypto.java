package com.qiangpiao.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.regex.Pattern;

/**
 * 敏感字段（身份证号 / 手机号）加解密、脱敏与格式强校验：对外明文，落库密文。
 *
 * <pre>
 *   算法：AES/ECB/PKCS5Padding + Base64，密文带 "ENC:" 前缀。
 *   - 选 ECB 是为了「确定性加密」：同一明文得到同一密文，
 *     这样常用乘车人去重、按证件号 / 手机号比对仍可在 SQL 层做等值比较；
 *   - 密文带前缀，未加密的历史明文数据不会被误解密（decrypt 原样返回），
 *     灰度切换无需停机迁移存量数据。
 *
 *   存密文的列：t_user.id_card / t_user.phone、t_order.id_card、t_passenger.id_card / t_passenger.phone。
 *   所有对外出口（接口 / 后台列表）统一脱敏：身份证前 4 后 4，手机号前 3 后 4。
 * </pre>
 */
@Slf4j
@Component
public class SensitiveCrypto {

    /** 密文前缀：用于区分「已加密」与「历史明文」，保证平滑迁移 */
    private static final String PREFIX = "ENC:";
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /** 手机号：1 开头，第二位按工信部已投放号段校验（3/4/5/6/7/8/9 下的有效子段） */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^1(3\\d|4[5-9]|5[0-35-9]|6[2567]|7[0-8]|8\\d|9[0-35-9])\\d{8}$");

    /** 18 位身份证校验码加权因子 */
    private static final int[] ID_WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
    /** 加权和 % 11 对应的校验码 */
    private static final char[] ID_CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    private static final DateTimeFormatter BIRTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final LocalDate BIRTH_MIN = LocalDate.of(1900, 1, 1);

    @Value("${crypto.aes-key:Qp@IdCard2026Key}")
    private String aesKey;

    /**
     * 开发测试白名单开关（crypto.test-whitelist）：
     * 开启时，身份证 111111 开头、手机号 111 开头跳过格式校验，方便造测试数据。
     * ⚠️ 生产环境必须改为 false，否则乱填的证件号能通过校验。
     */
    @Value("${crypto.test-whitelist:true}")
    private boolean testWhitelist;

    private SecretKeySpec keySpec;

    @PostConstruct
    public void init() {
        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            throw new IllegalStateException("crypto.aes-key 长度必须是 16 / 24 / 32 字节，当前：" + key.length);
        }
        this.keySpec = new SecretKeySpec(key, ALGORITHM);
    }

    // ==================== 加解密 ====================

    /**
     * 明文 → 密文（落库前调用）。
     *
     * @param plain 明文（身份证号 / 手机号）
     * @return 形如 {@code ENC:xxxx} 的密文；空值或已是密文则原样返回
     */
    public String encrypt(String plain) {
        if (!StringUtils.hasText(plain) || plain.startsWith(PREFIX)) {
            return plain;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            return PREFIX + Base64.getEncoder()
                    .encodeToString(cipher.doFinal(plain.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // 加密失败绝不能让下单 / 注册主流程中断，降级为原文（日志告警由监控发现）
            log.error("敏感字段加密失败，降级存储明文", e);
            return plain;
        }
    }

    /**
     * 密文 → 明文（读取后、业务使用前调用）。
     *
     * @param cipher 库里的值
     * @return 明文；非密文（历史明文数据）或解密失败则原样返回
     */
    public String decrypt(String cipher) {
        if (!StringUtils.hasText(cipher) || !cipher.startsWith(PREFIX)) {
            // 存量明文数据：直接返回，保证迁移期可读
            return cipher;
        }
        try {
            Cipher c = Cipher.getInstance(TRANSFORMATION);
            c.init(Cipher.DECRYPT_MODE, keySpec);
            return new String(c.doFinal(Base64.getDecoder().decode(cipher.substring(PREFIX.length()))),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("敏感字段解密失败，按原文处理（可能是密钥已更换）：{}", e.getMessage());
            return cipher;
        }
    }

    /** 是否为密文（带 ENC: 前缀） */
    public boolean isEncrypted(String value) {
        return StringUtils.hasText(value) && value.startsWith(PREFIX);
    }

    // ==================== 脱敏 ====================

    /** 库里的身份证密文 → 脱敏展示（前 4 后 4） */
    public String maskIdCard(String cipher) {
        return maskIdCardPlain(decrypt(cipher));
    }

    /** 身份证明文 → 脱敏展示（前 4 后 4） */
    public String maskIdCardPlain(String plain) {
        if (!StringUtils.hasText(plain) || plain.length() < 8) {
            return plain;
        }
        return plain.substring(0, 4) + "********" + plain.substring(plain.length() - 4);
    }

    /** 库里的手机号密文 → 脱敏展示（前 3 后 4） */
    public String maskPhone(String cipher) {
        return maskPhonePlain(decrypt(cipher));
    }

    /** 手机号明文 → 脱敏展示（前 3 后 4） */
    public String maskPhonePlain(String plain) {
        if (!StringUtils.hasText(plain) || plain.length() < 7) {
            return plain;
        }
        return plain.substring(0, 3) + "****" + plain.substring(plain.length() - 4);
    }

    // ==================== 格式强校验（位数 + 真实合法性） ====================

    /**
     * 身份证号合法性：不只校验位数。
     * <ul>
     *   <li>18 位：前 17 位必须全数字、出生日期必须是真实存在的日期且不晚于今天、最后 1 位必须等于 mod 11-2 算出的校验码</li>
     *   <li>15 位（老证）：全数字 + 出生日期合法（年份补 19）</li>
     * </ul>
     */
    public boolean validIdCard(String plain) {
        if (!StringUtils.hasText(plain)) {
            return false;
        }
        String id = plain.trim();
        // 开发测试白名单：111111 开头的 15 / 18 位号码直接放行（test-whitelist=true 时）
        if (testWhitelist && (id.length() == 18 || id.length() == 15) && id.startsWith("111111")) {
            return true;
        }
        if (id.length() == 18) {
            return validIdCard18(id);
        }
        if (id.length() == 15) {
            // 老身份证：1900 年代出生，无校验位，只能校验位数与出生日期
            return id.matches("\\d{15}") && validBirth("19" + id.substring(6, 12));
        }
        return false;
    }

    /** 兼容旧调用：身份证校验 */
    public boolean valid(String plain) {
        return validIdCard(plain);
    }

    private boolean validIdCard18(String id) {
        if (!id.matches("\\d{17}[0-9Xx]")) {
            return false;
        }
        // 出生日期必须是真实日期，且在 1900-01-01 与今天之间
        if (!validBirth(id.substring(6, 14))) {
            return false;
        }
        // mod 11-2 校验码
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            int digit = id.charAt(i) - '0';
            if (digit < 0 || digit > 9) {
                return false;
            }
            sum += digit * ID_WEIGHT[i];
        }
        char expect = ID_CHECK_CODE[sum % 11];
        return expect == Character.toUpperCase(id.charAt(17));
    }

    private boolean validBirth(String yyyyMMdd) {
        try {
            LocalDate birth = LocalDate.parse(yyyyMMdd, BIRTH_FORMATTER);
            return !birth.isBefore(BIRTH_MIN) && !birth.isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 手机号合法性：11 位 + 号段校验（第二位必须是已投放的号段），避免 11111111111 这种乱填通过。
     */
    public boolean validPhone(String plain) {
        if (!StringUtils.hasText(plain)) {
            return false;
        }
        String phone = plain.trim();
        // 开发测试白名单：111 开头的 11 位号码直接放行（test-whitelist=true 时）
        if (testWhitelist && phone.matches("\\d{11}") && phone.startsWith("111")) {
            return true;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }
}

package com.qiangpiao.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 退票 / 改签手续费计算（对齐 12306 阶梯退票费规则）。
 *
 * <pre>
 *   距开车时间            费率
 *   开车前 8 天（含）以上   0%（免费）
 *   48 小时 ~ 8 天         5%
 *   24 小时 ~ 48 小时      10%
 *   不足 24 小时           20%
 *
 *   尾数规则：以 5 角为单位（&lt;2.5 角舍去，2.5~7.5 角计 5 角，≥7.5 角进 1 元）
 *   最低 2 元；票价不足 2 元时按票价收取；手续费不超过票价
 * </pre>
 */
public final class RefundFeeUtil {

    /** 8 天（分钟） */
    public static final long MINUTES_8_DAYS = 8L * 24 * 60;
    /** 48 小时（分钟） */
    public static final long MINUTES_48_HOURS = 48L * 60;
    /** 24 小时（分钟） */
    public static final long MINUTES_24_HOURS = 24L * 60;

    public static final BigDecimal RATE_FREE = new BigDecimal("0");
    public static final BigDecimal RATE_5 = new BigDecimal("0.05");
    public static final BigDecimal RATE_10 = new BigDecimal("0.10");
    public static final BigDecimal RATE_20 = new BigDecimal("0.20");
    /** 开车后当日 24 点前退票 */
    public static final BigDecimal RATE_50 = new BigDecimal("0.50");

    /** 退票费最低 2 元 */
    public static final BigDecimal MIN_FEE = new BigDecimal("2.00");
    /** 尾数单位：5 角 */
    private static final BigDecimal HALF_YUAN = new BigDecimal("0.50");
    private static final BigDecimal QUARTER_YUAN = new BigDecimal("0.25");
    private static final BigDecimal THREE_QUARTER_YUAN = new BigDecimal("0.75");

    private RefundFeeUtil() {
    }

    /**
     * 按距开车时间取费率档位。
     *
     * @param minutesToDepart 距开车分钟数，已发车为负数
     */
    public static BigDecimal rate(long minutesToDepart) {
        if (minutesToDepart >= MINUTES_8_DAYS) {
            return RATE_FREE;
        }
        if (minutesToDepart >= MINUTES_48_HOURS) {
            return RATE_5;
        }
        if (minutesToDepart >= MINUTES_24_HOURS) {
            return RATE_10;
        }
        return RATE_20;
    }

    /**
     * 按费率计算手续费（含 5 角进位、最低 2 元、上限为票价）。
     *
     * @param price 计费基数（票价或差额）
     * @param rate  费率
     */
    public static BigDecimal feeByRate(BigDecimal price, BigDecimal rate) {
        BigDecimal base = price == null ? BigDecimal.ZERO : price.max(BigDecimal.ZERO);
        if (base.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal actualRate = rate == null ? RATE_FREE : rate;
        if (actualRate.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal raw = base.multiply(actualRate);
        BigDecimal fee = roundToFiveJiao(raw);
        // 票价不足 2 元按票价收；否则最低 2 元
        if (base.compareTo(MIN_FEE) < 0) {
            fee = base;
        } else if (fee.compareTo(MIN_FEE) < 0) {
            fee = MIN_FEE;
        }
        // 手续费不能超过票价
        if (fee.compareTo(base) > 0) {
            fee = base;
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 按距开车时间计算手续费。
     */
    public static BigDecimal fee(BigDecimal price, long minutesToDepart) {
        return feeByRate(price, rate(minutesToDepart));
    }

    /**
     * 尾数以 5 角为单位：&lt;2.5 角舍去，2.5~7.5 角计为 5 角，≥7.5 角进为 1 元。
     */
    public static BigDecimal roundToFiveJiao(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal units = amount.divide(HALF_YUAN, 0, RoundingMode.DOWN);
        BigDecimal remainder = amount.subtract(units.multiply(HALF_YUAN));
        if (remainder.compareTo(QUARTER_YUAN) < 0) {
            return units.multiply(HALF_YUAN);
        }
        if (remainder.compareTo(THREE_QUARTER_YUAN) < 0) {
            return units.multiply(HALF_YUAN).add(HALF_YUAN);
        }
        return units.multiply(HALF_YUAN).add(BigDecimal.ONE);
    }

    /** 费率档位文案（展示用） */
    public static String rateText(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) == 0) {
            return "开车前 8 天以上，免收手续费";
        }
        if (rate.compareTo(RATE_5) == 0) {
            return "开车前 48 小时 ~ 8 天，收取 5%";
        }
        if (rate.compareTo(RATE_10) == 0) {
            return "开车前 24 ~ 48 小时，收取 10%";
        }
        if (rate.compareTo(RATE_50) == 0) {
            return "开车后当日 24 点前退票，收取 50%";
        }
        return "开车前不足 24 小时，收取 20%";
    }
}

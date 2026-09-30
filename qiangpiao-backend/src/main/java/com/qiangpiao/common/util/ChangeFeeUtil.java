package com.qiangpiao.common.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 改签手续费计算（对齐 12306 改签规则）。
 *
 * <pre>
 *   一张车票只能改签 1 次（由 AfterSaleServiceImpl 校验）
 *
 *   改签场景                                 费率
 *   开车前 ≥48h，改任意预售期车次              0%
 *   开车前 &lt;48h，改原乘车日（含当天）列车       0%
 *   开车前 24h ~ 48h，改到原日期之后            5%
 *   开车前 &lt;24h，改到原日期之后                15%
 *   开车后当日 24 点前，改当天其他列车           0%
 *   开车后当日 24 点前，改次日及以后             40%
 *   开车后超过当日 24 点                       不再办理改签
 *
 *   计费基数 = 新旧两张票里【较低】的那张票价（12306 规则）
 *   尾数规则沿用退票费：5 角为单位、最低 2 元、不超过基数
 * </pre>
 */
public final class ChangeFeeUtil {

    /** 开车前 24~48 小时改签到乘车日之后 */
    public static final BigDecimal RATE_5 = RefundFeeUtil.RATE_5;
    /** 开车前不足 24 小时改签到乘车日之后 */
    public static final BigDecimal RATE_15 = new BigDecimal("0.15");
    /** 开车后当日 24 点前改签到次日及以后 */
    public static final BigDecimal RATE_40 = new BigDecimal("0.40");

    private ChangeFeeUtil() {
    }

    /**
     * 改签判定结果。
     */
    public static final class Result {

        private static final Result NOT_ALLOWED =
                new Result(false, RefundFeeUtil.RATE_FREE, "当前时间不在改签办理窗口内");

        private final boolean allowed;
        private final BigDecimal rate;
        private final String ruleText;

        private Result(boolean allowed, BigDecimal rate, String ruleText) {
            this.allowed = allowed;
            this.rate = rate;
            this.ruleText = ruleText;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public BigDecimal getRate() {
            return rate;
        }

        public String getRuleText() {
            return ruleText;
        }

        public static Result notAllowed(String reason) {
            return new Result(false, RefundFeeUtil.RATE_FREE, reason);
        }

        public static Result getNotAllowed() {
            return NOT_ALLOWED;
        }
    }

    /**
     * 判定改签场景与费率。
     *
     * @param now           当前时间
     * @param oldDepartAt   原车次发车时刻
     * @param oldDepartDate 原车票乘车日期
     * @param newDepartDate 新车票乘车日期
     */
    public static Result calc(LocalDateTime now, LocalDateTime oldDepartAt,
                              LocalDate oldDepartDate, LocalDate newDepartDate) {
        // 时刻缺失（历史脏数据）时不做拦截，按免费处理，避免用户无法改签
        if (now == null || oldDepartAt == null || oldDepartDate == null || newDepartDate == null) {
            return new Result(true, RefundFeeUtil.RATE_FREE, "改签免收手续费");
        }
        if (!oldDepartAt.isAfter(now)) {
            return calcAfterDepart(now, oldDepartDate, newDepartDate);
        }
        return calcBeforeDepart(now, oldDepartAt, oldDepartDate, newDepartDate);
    }

    /** 开车前 */
    private static Result calcBeforeDepart(LocalDateTime now, LocalDateTime oldDepartAt,
                                           LocalDate oldDepartDate, LocalDate newDepartDate) {
        long minutes = ChronoUnit.MINUTES.between(now, oldDepartAt);
        if (minutes >= RefundFeeUtil.MINUTES_48_HOURS) {
            return new Result(true, RefundFeeUtil.RATE_FREE, "开车前 48 小时以上改签，免收改签费");
        }
        // 改到原乘车日（含当天）：不论距开车多久都免费
        if (!newDepartDate.isAfter(oldDepartDate)) {
            return new Result(true, RefundFeeUtil.RATE_FREE,
                    "开车前不足 48 小时改签至原乘车日（含当天）车次，免收改签费");
        }
        if (minutes >= RefundFeeUtil.MINUTES_24_HOURS) {
            return new Result(true, RATE_5, "开车前 24~48 小时改签至乘车日之后，收取 5% 改签费");
        }
        return new Result(true, RATE_15, "开车前不足 24 小时改签至乘车日之后，收取 15% 改签费");
    }

    /** 开车后：仅当日 24 点前可办，改当天其他列车免费，改次日及以后 40% */
    private static Result calcAfterDepart(LocalDateTime now, LocalDate oldDepartDate, LocalDate newDepartDate) {
        if (!now.toLocalDate().equals(oldDepartDate)) {
            return Result.notAllowed("列车已发车且不在乘车当日，开车后仅可在乘车当日 24 点前改签");
        }
        if (!newDepartDate.isAfter(oldDepartDate)) {
            return new Result(true, RefundFeeUtil.RATE_FREE, "开车后当日 24 点前改签当日其他列车，免收改签费");
        }
        return new Result(true, RATE_40, "开车后当日 24 点前改签次日及以后列车，收取 40% 改签费");
    }

    /**
     * 改签费计费基数：新旧两张票里较低的那张票价。
     */
    public static BigDecimal base(BigDecimal oldPrice, BigDecimal newPrice) {
        BigDecimal oldBase = oldPrice == null ? BigDecimal.ZERO : oldPrice.max(BigDecimal.ZERO);
        BigDecimal newBase = newPrice == null ? BigDecimal.ZERO : newPrice.max(BigDecimal.ZERO);
        return oldBase.min(newBase);
    }

    /**
     * 按费率计算改签费（5 角进位、最低 2 元、不超过基数）。
     */
    public static BigDecimal fee(BigDecimal oldPrice, BigDecimal newPrice, BigDecimal rate) {
        return RefundFeeUtil.feeByRate(base(oldPrice, newPrice), rate);
    }
}

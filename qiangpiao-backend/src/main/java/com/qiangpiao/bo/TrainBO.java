package com.qiangpiao.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 车次 BO：Service 层之间传递（Controller 不直接使用，统一转 VO 出参）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainBO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String trainNo;
    private String trainType;
    private Long fromStationId;
    private String fromStationName;
    private Long toStationId;
    private String toStationName;
    private LocalDate departDate;
    private LocalTime departTime;
    private LocalTime arriveTime;
    private Integer durationMinutes;
    private Integer status;
    /** 售票开始时间：为空表示不限制 */
    private LocalDateTime saleStartTime;
    /** 售票结束时间：为空表示不限制 */
    private LocalDateTime saleEndTime;
    /** 该车次各席别库存 */
    private List<TrainStockBO> stocks = new ArrayList<>();

    public boolean onSale() {
        return status != null && status == 1;
    }

    /**
     * 是否处于售卖时间窗口内（t_train.sale_start_time ~ sale_end_time）。
     * 两列都为空表示不限制（默认一直卖）。
     */
    public boolean inSaleWindow(LocalDateTime now) {
        if (saleStartTime != null && now.isBefore(saleStartTime)) {
            return false;
        }
        return saleEndTime == null || !now.isAfter(saleEndTime);
    }

    /** 售卖窗口未开始的原因文案；已开始或不限时返回 null */
    public String saleWindowTip(LocalDateTime now) {
        if (saleStartTime != null && now.isBefore(saleStartTime)) {
            return "售票开始时间 " + saleStartTime.toString().replace('T', ' ');
        }
        if (saleEndTime != null && now.isAfter(saleEndTime)) {
            return "售票已于 " + saleEndTime.toString().replace('T', ' ') + " 结束";
        }
        return null;
    }

    /** 发车时刻（日期 + 时间） */
    public LocalDateTime departAt() {
        if (departDate == null || departTime == null) {
            return null;
        }
        return LocalDateTime.of(departDate, departTime);
    }

    /** 是否已发车（含正好到点） */
    public boolean departed(LocalDateTime now) {
        LocalDateTime departAt = departAt();
        return departAt == null || !departAt.isAfter(now);
    }

    /**
     * 到达时刻（日期 + 时间）；到达时间不晚于发车时间时视为次日到达，自动 +1 天。
     */
    public LocalDateTime arriveAt() {
        if (departDate == null || arriveTime == null) {
            return null;
        }
        LocalDateTime arrive = LocalDateTime.of(departDate, arriveTime);
        if (departTime != null && !arriveTime.isAfter(departTime)) {
            arrive = arrive.plusDays(1);
        }
        return arrive;
    }

    /** 是否已到达终点（下车）：用于判断用户行程是否结束 */
    public boolean arrived(LocalDateTime now) {
        LocalDateTime arriveAt = arriveAt();
        return arriveAt == null || !arriveAt.isAfter(now);
    }

    /** 距发车还剩多少分钟（时刻缺失时返回 Long.MAX_VALUE） */
    public long minutesToDepart(LocalDateTime now) {
        LocalDateTime departAt = departAt();
        return departAt == null ? Long.MAX_VALUE : ChronoUnit.MINUTES.between(now, departAt);
    }

    public String durationText() {
        if (durationMinutes == null) {
            return "";
        }
        return (durationMinutes / 60) + "小时" + (durationMinutes % 60) + "分";
    }
}

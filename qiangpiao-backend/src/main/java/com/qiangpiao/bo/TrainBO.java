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
    /** 该车次各席别库存 */
    private List<TrainStockBO> stocks = new ArrayList<>();

    public boolean onSale() {
        return status != null && status == 1;
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

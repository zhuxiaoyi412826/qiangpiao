package com.qiangpiao.service.impl;

import com.qiangpiao.cache.MultiLevelCacheService;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.mapper.AdminMapper;
import com.qiangpiao.mapper.TrainMapper;
import com.qiangpiao.service.TrainScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 日期班次生成实现：以「每个车次号最早一天的记录」为模板，复制出未来每天的实际班次。
 * <p>
 * 复制内容：车次基本信息（不含日期）+ 各席别库存（余量重置为总量）+ 座位图（全部重置为可售）+ 时刻表（经停站）。
 * 幂等依赖唯一键 uk_train_no_date，已存在该日期班次时直接跳过。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainScheduleServiceImpl implements TrainScheduleService {

    private final TrainMapper trainMapper;
    private final AdminMapper adminMapper;
    private final MultiLevelCacheService cacheService;

    /** 生成天数：与可查询天数一致（ticket.query-days，默认 30 天） */
    @Value("${ticket.query-days}")
    private int scheduleDays;

    @Override
    public int generateFutureTrains() {
        LocalDate today = LocalDate.now();
        List<TrainDO> templates = trainMapper.selectTemplates();
        if (templates == null || templates.isEmpty()) {
            log.warn("未找到在售车次模板，跳过班次生成");
            return 0;
        }
        int created = 0;
        for (TrainDO template : templates) {
            for (int i = 0; i < scheduleDays; i++) {
                LocalDate date = today.plusDays(i);
                try {
                    if (generateDailyTrain(template.getId(), date) != null) {
                        created++;
                    }
                } catch (Exception e) {
                    // 单个班次失败不影响其他日期（多数是并发下的唯一键冲突）
                    log.warn("生成班次失败：templateId={}, trainNo={}, date={}, msg={}",
                            template.getId(), template.getTrainNo(), date, e.getMessage());
                }
            }
        }
        if (created > 0) {
            // 列表按日期分 key、详情按 id，统一失效
            cacheService.evictPrefix(Constants.CACHE_TRAIN_LIST);
            cacheService.evictPrefix(Constants.CACHE_TRAIN_DETAIL);
            log.info("每日班次生成完成：模板数={}, 新增班次={}, 覆盖 {} ~ {}",
                    templates.size(), created, today, today.plusDays(scheduleDays - 1));
        }
        return created;
    }

    @Override
    public Long generateDailyTrain(Long templateTrainId, LocalDate date) {
        if (templateTrainId == null || date == null) {
            return null;
        }
        TrainDO template = trainMapper.selectById(templateTrainId);
        if (template == null) {
            return null;
        }
        // 幂等：该日期班次已存在则跳过
        if (trainMapper.selectIdByNoAndDate(template.getTrainNo(), date) != null) {
            return null;
        }
        if (adminMapper.copyTrain(templateTrainId, date) <= 0) {
            return null;
        }
        Long newTrainId = adminMapper.lastInsertId();
        adminMapper.copyStock(templateTrainId, newTrainId);
        adminMapper.copySeats(templateTrainId, newTrainId);
        // 时刻表必须一起复制：没有经停站，中转方案建不出图，区间票也会退化成全程票
        adminMapper.copyStops(templateTrainId, newTrainId);
        return newTrainId;
    }
}

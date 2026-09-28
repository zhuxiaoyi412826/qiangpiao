package com.qiangpiao.service;

import java.time.LocalDate;

/**
 * 日期班次（调度）服务：车次模板 + 每日实际运行班次分开。
 * <pre>
 *   t_train 里同一 train_no 有多条记录，一条 = 一天的实际班次（uk_train_no_date）。
 *   本服务按模板批量复制出未来 N 天的班次（含库存与座位），保证：
 *     - 每天都有车次可查询（可查询范围 ticket.query-days，默认 30 天）
 *     - 每天都有车次发车售卖（购买仍受预售期 ticket.presale-days 限制，默认 14 天）
 * </pre>
 */
public interface TrainScheduleService {

    /**
     * 为所有在售车次模板生成「今天起 N 天」的班次（N = ticket.query-days），幂等。
     *
     * @return 新增班次数
     */
    int generateFutureTrains();

    /**
     * 生成模板车次在指定日期的班次（复制车次、库存、座位），已存在则跳过。
     *
     * @return 新班次 id；已存在或参数非法返回 null
     */
    Long generateDailyTrain(Long templateTrainId, LocalDate date);
}

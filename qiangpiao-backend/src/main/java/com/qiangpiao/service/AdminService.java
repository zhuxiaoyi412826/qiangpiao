package com.qiangpiao.service;

import com.qiangpiao.dataobject.AnnouncementDO;
import com.qiangpiao.dataobject.CarriageDO;
import com.qiangpiao.dataobject.LineDO;
import com.qiangpiao.dataobject.LineStationDO;
import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.StationDO;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainStopDO;
import com.qiangpiao.dataobject.UserDO;
import com.qiangpiao.vo.SeckillFlowVO;
import com.qiangpiao.dto.AdminOrderQueryDTO;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.vo.AdminOrderVO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理后台服务：车站 / 线路 / 车次（时刻表·车厢·停运·排班）/ 订单 / 用户 / 公告 / 监控统计。
 */
public interface AdminService {

    // ==================== 车站 ====================

    List<StationDO> stations();

    StationDO saveStation(StationDO station);

    /** 停用 / 启用车站 */
    void updateStationStatus(Long id, Integer status);

    // ==================== 线路 ====================

    List<LineDO> lines();

    LineDO saveLine(LineDO line);

    void deleteLine(Long id);

    List<LineStationDO> lineStations(Long lineId);

    /** 全量覆盖某条线路的途经站（含顺序） */
    void saveLineStations(Long lineId, List<LineStationDO> stations);

    // ==================== 车次 ====================

    PageResult<TrainDO> trains(Integer pageNum, Integer pageSize);

    /** 停开某一天车次（停运）/ 恢复 */
    void updateTrainStatus(Long trainId, Integer status);

    /**
     * 设置车次售卖时间窗口（t_train.sale_start_time / sale_end_time）。
     * 两个时间都可以为空，为空表示不限制；窗口外不允许抢票 / 下单。
     */
    void updateSaleWindow(Long trainId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);

    List<TrainStopDO> stops(Long trainId);

    /** 全量覆盖车次时刻表 */
    void saveStops(Long trainId, List<TrainStopDO> stops);

    List<CarriageDO> carriages(Long trainId);

    /** 全量覆盖车厢编排 */
    void saveCarriages(Long trainId, List<CarriageDO> carriages);

    /**
     * 按日期生成当日可售班次：复制车次模板（含库存与座位），实现「模板 + 每日排班」分离。
     *
     * @return 新生成的班次车次ID
     */
    Long generateDailyTrain(Long sourceTrainId, LocalDate date);

    // ==================== 票价 ====================

    /** 票价调整：按车次席别改价 */
    void updateStockPrice(Long stockId, java.math.BigDecimal price);

    /** 调整某车次席别的总座位数（同步修正余票） */
    void updateStockTotal(Long stockId, Integer totalCount);

    // ==================== 订单 ====================

    PageResult<AdminOrderVO> orders(AdminOrderQueryDTO query);

    /**
     * 抢票流水（后台排查用：谁在什么时候抢了哪趟车、成功还是失败、耗时多少）
     */
    PageResult<SeckillFlowVO> seckillFlows(Long userId, Long trainId, Integer status,
                                           Integer pageNum, Integer pageSize);

    AdminOrderVO orderDetail(String orderNo);

    List<OrderLogDO> orderLogs(String orderNo);

    List<OrderChangeDO> orderChanges(String orderNo);

    // ==================== 用户 ====================

    PageResult<UserDO> users(String keyword, Integer pageNum, Integer pageSize);

    /** 封禁 / 解封用户（封禁会同步把该用户 token 全部拉黑） */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 强制下线：拉黑该用户当前所有 token，下一次请求即 401。
     *
     * @return 被拉黑的 token 数量（0 表示对方没有在线 token）
     */
    int kickUser(Long userId);

    // ==================== 公告 ====================

    PageResult<AnnouncementDO> announcements(Integer pageNum, Integer pageSize);

    AnnouncementDO saveAnnouncement(AnnouncementDO announcement);

    void deleteAnnouncement(Long id);

    List<AnnouncementDO> activeAnnouncements(Integer limit);

    // ==================== 监控 / 报表 ====================

    /**
     * 余票监控：各车次各席别剩余座位（分页，避免一次性拉全表）。
     *
     * @param trainNo 车次号模糊筛选，可为空
     */
    PageResult<Map<String, Object>> stockMonitor(Integer pageNum, Integer pageSize, String trainNo);

    /** 锁票管理：下单后临时锁定、超时自动释放的座位 */
    List<SeatDO> lockedSeats(Integer limit);

    /** 运营概览：营收、退票、用户、订单 */
    Map<String, Object> stats();

    List<Map<String, Object>> dailySales();

    List<Map<String, Object>> trainSales();

    List<Map<String, Object>> userGrowth();
}

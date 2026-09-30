package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.SensitiveCrypto;
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
import com.qiangpiao.dto.AdminOrderQueryDTO;
import com.qiangpiao.mapper.AdminMapper;
import com.qiangpiao.service.AdminService;
import com.qiangpiao.service.OrderLogService;
import com.qiangpiao.service.SeckillFlowService;
import com.qiangpiao.service.TokenService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.AdminOrderVO;
import com.qiangpiao.vo.SeckillFlowVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理后台服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final TrainService trainService;
    private final SensitiveCrypto crypto;
    private final OrderLogService orderLogService;
    private final SeckillFlowService seckillFlowService;
    private final TokenService tokenService;

    // ==================== 车站 ====================

    @Override
    public List<StationDO> stations() {
        return adminMapper.listStations();
    }

    @Override
    public StationDO saveStation(StationDO station) {
        if (station == null || !StringUtils.hasText(station.getStationName())) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (station.getId() == null) {
            adminMapper.insertStation(station);
        } else {
            adminMapper.updateStation(station);
        }
        return station;
    }

    @Override
    public void updateStationStatus(Long id, Integer status) {
        adminMapper.updateStationStatus(id, status);
        log.info("车站状态变更：stationId={}, status={}", id, status);
    }

    // ==================== 线路 ====================

    @Override
    public List<LineDO> lines() {
        return adminMapper.listLines();
    }

    @Override
    public LineDO saveLine(LineDO line) {
        if (line == null || !StringUtils.hasText(line.getLineName())) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (line.getId() == null) {
            adminMapper.insertLine(line);
        } else {
            adminMapper.updateLine(line);
        }
        return line;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLine(Long id) {
        adminMapper.deleteLineStations(id);
        adminMapper.deleteLine(id);
    }

    @Override
    public List<LineStationDO> lineStations(Long lineId) {
        return adminMapper.listLineStations(lineId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLineStations(Long lineId, List<LineStationDO> stations) {
        adminMapper.deleteLineStations(lineId);
        if (stations == null || stations.isEmpty()) {
            return;
        }
        int order = 1;
        for (LineStationDO station : stations) {
            station.setLineId(lineId);
            if (station.getStopOrder() == null) {
                station.setStopOrder(order);
            }
            adminMapper.insertLineStation(station);
            order++;
        }
    }

    // ==================== 车次 ====================

    @Override
    public PageResult<TrainDO> trains(Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
        List<TrainDO> list = adminMapper.listTrains((long) (pn - 1) * ps, (long) ps);
        long total = adminMapper.countTrains();
        return PageResult.of(pn, ps, total, list == null ? new ArrayList<>() : list);
    }

    @Override
    public void updateTrainStatus(Long trainId, Integer status) {
        adminMapper.updateTrainStatus(trainId, status);
        trainService.evictTrainCache(trainId);
        log.info("车次停运状态变更：trainId={}, status={}", trainId, status);
    }

    @Override
    public void updateSaleWindow(Long trainId, java.time.LocalDateTime startTime, java.time.LocalDateTime endTime) {
        adminMapper.updateSaleWindow(trainId, startTime, endTime);
        trainService.evictTrainCache(trainId);
        log.info("车次售卖时间窗口变更：trainId={}, start={}, end={}", trainId, startTime, endTime);
    }

    @Override
    public List<TrainStopDO> stops(Long trainId) {
        return adminMapper.listStops(trainId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveStops(Long trainId, List<TrainStopDO> stops) {
        adminMapper.deleteStops(trainId);
        if (stops == null || stops.isEmpty()) {
            return;
        }
        int order = 1;
        for (TrainStopDO stop : stops) {
            stop.setTrainId(trainId);
            if (stop.getStopOrder() == null) {
                stop.setStopOrder(order);
            }
            adminMapper.insertStop(stop);
            order++;
        }
    }

    @Override
    public List<CarriageDO> carriages(Long trainId) {
        return adminMapper.listCarriages(trainId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCarriages(Long trainId, List<CarriageDO> carriages) {
        adminMapper.deleteCarriages(trainId);
        if (carriages == null || carriages.isEmpty()) {
            return;
        }
        for (CarriageDO carriage : carriages) {
            carriage.setTrainId(trainId);
            adminMapper.insertCarriage(carriage);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateDailyTrain(Long sourceTrainId, LocalDate date) {
        if (sourceTrainId == null || date == null) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        int rows = adminMapper.copyTrain(sourceTrainId, date);
        if (rows <= 0) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        Long newTrainId = adminMapper.lastInsertId();
        adminMapper.copyStock(sourceTrainId, newTrainId);
        adminMapper.copySeats(sourceTrainId, newTrainId);
        log.info("生成当日班次：sourceTrainId={}, date={}, newTrainId={}", sourceTrainId, date, newTrainId);
        return newTrainId;
    }

    // ==================== 票价 ====================

    @Override
    public void updateStockPrice(Long stockId, BigDecimal price) {
        if (stockId == null || price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        adminMapper.updateStockPrice(stockId, price);
        log.info("票价调整：stockId={}, price={}", stockId, price);
    }

    @Override
    public void updateStockTotal(Long stockId, Integer totalCount) {
        if (stockId == null || totalCount == null || totalCount < 0) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        adminMapper.updateStockTotal(stockId, totalCount);
        log.info("座位总数调整：stockId={}, totalCount={}", stockId, totalCount);
    }

    // ==================== 订单 ====================

    @Override
    public PageResult<SeckillFlowVO> seckillFlows(Long userId, Long trainId, Integer status,
                                                  Integer pageNum, Integer pageSize) {
        return seckillFlowService.page(userId, trainId, status, pageNum, pageSize);
    }

    @Override
    public PageResult<AdminOrderVO> orders(AdminOrderQueryDTO query) {
        int pn = (query.getPageNum() == null || query.getPageNum() < 1) ? 1 : query.getPageNum();
        int ps = (query.getPageSize() == null || query.getPageSize() < 1) ? 10 : Math.min(query.getPageSize(), 100);
        query.setPageNum(pn);
        query.setPageSize(ps);
        query.setOffset((long) (pn - 1) * ps);
        query.setLimit((long) ps);
        // 手机号加密存储：查询条件转成密文等值匹配（明文条件保留用于匹配存量数据）
        String phone = query.getPhone();
        query.setPhoneCipher(phone == null || phone.isEmpty() ? null : crypto.encrypt(phone.trim()));

        List<AdminOrderVO> list = adminMapper.listOrders(query);
        long total = adminMapper.countOrders(query);
        if (list != null) {
            for (AdminOrderVO vo : list) {
                vo.setStatusText(OrderServiceImpl.statusText(vo.getStatus()));
                // 库里是密文，后台展示同样只给脱敏值
                vo.setIdCard(crypto.maskIdCard(vo.getIdCard()));
                vo.setPhone(crypto.maskPhone(vo.getPhone()));
            }
        }
        return PageResult.of(pn, ps, total, list == null ? new ArrayList<>() : list);
    }

    @Override
    public AdminOrderVO orderDetail(String orderNo) {
        AdminOrderQueryDTO query = new AdminOrderQueryDTO();
        query.setOrderNo(orderNo);
        query.setOffset(0L);
        query.setLimit(1L);
        List<AdminOrderVO> list = adminMapper.listOrders(query);
        if (list == null || list.isEmpty()) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        AdminOrderVO vo = list.get(0);
        vo.setStatusText(OrderServiceImpl.statusText(vo.getStatus()));
        vo.setIdCard(crypto.maskIdCard(vo.getIdCard()));
        vo.setPhone(crypto.maskPhone(vo.getPhone()));
        return vo;
    }

    @Override
    public List<OrderLogDO> orderLogs(String orderNo) {
        return orderLogService.timeline(orderNo);
    }

    @Override
    public List<OrderChangeDO> orderChanges(String orderNo) {
        return adminMapper.listOrderChanges(orderNo);
    }

    // ==================== 用户 ====================

    @Override
    public PageResult<UserDO> users(String keyword, Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
        // keyword 本身就是手机号时，用密文等值补一个匹配条件（加密后无法 LIKE）
        String phoneCipher = (keyword != null && crypto.validPhone(keyword)) ? crypto.encrypt(keyword) : null;
        List<UserDO> list = adminMapper.listUsers(keyword, phoneCipher, (long) (pn - 1) * ps, (long) ps);
        long total = adminMapper.countUsers(keyword, phoneCipher);
        // 不向前端返回密码等敏感字段；身份证 / 手机号解密后脱敏
        if (list != null) {
            for (UserDO user : list) {
                user.setPassword(null);
                user.setIdCard(crypto.maskIdCard(user.getIdCard()));
                user.setPhone(crypto.maskPhone(user.getPhone()));
            }
        }
        return PageResult.of(pn, ps, total, list == null ? new ArrayList<>() : list);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (Constants.PLATFORM_USER_ID.equals(userId) && status != null && status == 0) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        adminMapper.updateUserStatus(userId, status);
        // 封号要立即生效：否则被封用户手里的 token 还能一直用到自然过期
        if (status.intValue() == Constants.USER_STATUS_DISABLED) {
            int kicked = tokenService.kickUser(userId);
            log.info("封号已同步踢下线：userId={}, 失效 token 数={}", userId, kicked);
        }
        log.info("用户状态变更：userId={}, status={}", userId, status);
    }

    @Override
    public int kickUser(Long userId) {
        if (userId == null) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        int kicked = tokenService.kickUser(userId);
        log.info("管理端强制下线：userId={}, 失效 token 数={}", userId, kicked);
        return kicked;
    }

    // ==================== 公告 ====================

    @Override
    public PageResult<AnnouncementDO> announcements(Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 50);
        List<AnnouncementDO> list = adminMapper.listAnnouncements((long) (pn - 1) * ps, (long) ps);
        long total = adminMapper.countAnnouncements();
        return PageResult.of(pn, ps, total, list == null ? new ArrayList<>() : list);
    }

    @Override
    public AnnouncementDO saveAnnouncement(AnnouncementDO announcement) {
        if (announcement == null || !StringUtils.hasText(announcement.getTitle())) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (announcement.getId() == null) {
            adminMapper.insertAnnouncement(announcement);
        } else {
            adminMapper.updateAnnouncement(announcement);
        }
        return announcement;
    }

    @Override
    public void deleteAnnouncement(Long id) {
        adminMapper.deleteAnnouncement(id);
    }

    @Override
    public List<AnnouncementDO> activeAnnouncements(Integer limit) {
        int size = (limit == null || limit < 1) ? 5 : Math.min(limit, 20);
        List<AnnouncementDO> list = adminMapper.listActiveAnnouncements((long) size);
        return list == null ? Collections.emptyList() : list;
    }

    // ==================== 监控 / 报表 ====================

    @Override
    public PageResult<Map<String, Object>> stockMonitor(Integer pageNum, Integer pageSize, String trainNo) {
        int page = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        List<Map<String, Object>> list = adminMapper.stockMonitor(trainNo, (long) (page - 1) * size, (long) size);
        long total = adminMapper.countStockMonitor(trainNo);
        return PageResult.of(page, size, total, list == null ? Collections.emptyList() : list);
    }

    @Override
    public List<SeatDO> lockedSeats(Integer limit) {
        int size = (limit == null || limit < 1) ? 50 : Math.min(limit, 200);
        return adminMapper.listLockedSeats((long) size);
    }

    @Override
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> revenue = adminMapper.revenue();
        Map<String, Object> userSummary = adminMapper.userSummary();
        result.put("revenue", revenue == null ? Collections.emptyMap() : revenue);
        result.put("users", userSummary == null ? Collections.emptyMap() : userSummary);
        return result;
    }

    @Override
    public List<Map<String, Object>> dailySales() {
        return adminMapper.dailySales();
    }

    @Override
    public List<Map<String, Object>> trainSales() {
        return adminMapper.trainSales();
    }

    @Override
    public List<Map<String, Object>> userGrowth() {
        return adminMapper.userGrowth();
    }
}

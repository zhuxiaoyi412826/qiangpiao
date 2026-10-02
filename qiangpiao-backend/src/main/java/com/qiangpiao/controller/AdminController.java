package com.qiangpiao.controller;

import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.R;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.dataobject.AnnouncementDO;
import com.qiangpiao.dataobject.CarriageDO;
import com.qiangpiao.dataobject.LineDO;
import com.qiangpiao.dataobject.LineStationDO;
import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.StationDO;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainSegmentStockDO;
import com.qiangpiao.dataobject.TrainStopDO;
import com.qiangpiao.dataobject.UserDO;
import com.qiangpiao.dto.AdminOrderQueryDTO;
import com.qiangpiao.service.AdminService;
import com.qiangpiao.service.AfterSaleService;
import com.qiangpiao.service.RateLimitService;
import com.qiangpiao.service.RiskControlService;
import com.qiangpiao.service.SegmentStockService;
import com.qiangpiao.vo.AdminOrderVO;
import com.qiangpiao.vo.SeckillFlowVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 管理后台接口：需 ROLE_ADMIN。
 * 覆盖车站、线路、车次（时刻表 / 车厢 / 停运 / 排班）、订单、用户、公告、监控与报表。
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "管理后台")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AfterSaleService afterSaleService;
    private final RateLimitService rateLimitService;
    private final RiskControlService riskControlService;
    private final SegmentStockService segmentStockService;

    // ==================== 车站 ====================

    @GetMapping("/stations")
    @ApiOperation("车站列表（含停用）")
    public R<List<StationDO>> stations() {
        return R.ok(adminService.stations());
    }

    @PostMapping("/stations")
    @ApiOperation("新增 / 编辑车站")
    public R<StationDO> saveStation(@RequestBody StationDO station) {
        return R.ok(adminService.saveStation(station));
    }

    @PutMapping("/stations/{id}/status")
    @ApiOperation("停用 / 启用车站")
    public R<Boolean> updateStationStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateStationStatus(id, status);
        return R.ok(true);
    }

    // ==================== 线路 ====================

    @GetMapping("/lines")
    @ApiOperation("线路列表")
    public R<List<LineDO>> lines() {
        return R.ok(adminService.lines());
    }

    @PostMapping("/lines")
    @ApiOperation("新增 / 编辑线路")
    public R<LineDO> saveLine(@RequestBody LineDO line) {
        return R.ok(adminService.saveLine(line));
    }

    @DeleteMapping("/lines/{id}")
    @ApiOperation("删除线路")
    public R<Boolean> deleteLine(@PathVariable Long id) {
        adminService.deleteLine(id);
        return R.ok(true);
    }

    @GetMapping("/lines/{lineId}/stations")
    @ApiOperation("线路途经站")
    public R<List<LineStationDO>> lineStations(@PathVariable Long lineId) {
        return R.ok(adminService.lineStations(lineId));
    }

    @PostMapping("/lines/{lineId}/stations")
    @ApiOperation("保存线路途经站（全量覆盖，按数组顺序）")
    public R<Boolean> saveLineStations(@PathVariable Long lineId, @RequestBody List<LineStationDO> stations) {
        adminService.saveLineStations(lineId, stations);
        return R.ok(true);
    }

    // ==================== 车次 ====================

    @GetMapping("/trains")
    @ApiOperation("车次列表（含每日班次）")
    public R<PageResult<TrainDO>> trains(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.trains(pageNum, pageSize));
    }

    @PutMapping("/trains/{id}/status")
    @ApiOperation("停开 / 恢复某天车次")
    public R<Boolean> updateTrainStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateTrainStatus(id, status);
        return R.ok(true);
    }

    @GetMapping("/trains/{id}/stops")
    @ApiOperation("车次时刻表")
    public R<List<TrainStopDO>> stops(@PathVariable Long id) {
        return R.ok(adminService.stops(id));
    }

    @PostMapping("/trains/{id}/stops")
    @ApiOperation("保存车次时刻表（全量覆盖）")
    public R<Boolean> saveStops(@PathVariable Long id, @RequestBody List<TrainStopDO> stops) {
        adminService.saveStops(id, stops);
        return R.ok(true);
    }

    @GetMapping("/trains/{id}/carriages")
    @ApiOperation("车次车厢编排")
    public R<List<CarriageDO>> carriages(@PathVariable Long id) {
        return R.ok(adminService.carriages(id));
    }

    @PostMapping("/trains/{id}/carriages")
    @ApiOperation("保存车厢编排（全量覆盖）")
    public R<Boolean> saveCarriages(@PathVariable Long id, @RequestBody List<CarriageDO> carriages) {
        adminService.saveCarriages(id, carriages);
        return R.ok(true);
    }

    @PostMapping("/trains/{id}/schedule")
    @ApiOperation("按日期生成当日班次（复制车次模板，含库存与座位）")
    public R<Long> generateDailyTrain(@PathVariable Long id,
                                      @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(adminService.generateDailyTrain(id, date));
    }

    // ==================== 票价 ====================

    @PutMapping("/stocks/{id}/price")
    @ApiOperation("票价调整")
    public R<Boolean> updateStockPrice(@PathVariable Long id, @RequestParam java.math.BigDecimal price) {
        adminService.updateStockPrice(id, price);
        return R.ok(true);
    }

    @PutMapping("/stocks/{id}/total")
    @ApiOperation("调整座位总数")
    public R<Boolean> updateStockTotal(@PathVariable Long id, @RequestParam Integer totalCount) {
        adminService.updateStockTotal(id, totalCount);
        return R.ok(true);
    }

    // ==================== 订单 ====================

    @GetMapping("/orders")
    @ApiOperation("订单列表：订单号 / 手机号 / 乘客 / 状态 / 日期筛选")
    public R<PageResult<AdminOrderVO>> orders(AdminOrderQueryDTO query) {
        return R.ok(adminService.orders(query));
    }

    @GetMapping("/orders/{orderNo}")
    @ApiOperation("订单详情（含乘客与座位）")
    public R<AdminOrderVO> orderDetail(@PathVariable String orderNo) {
        return R.ok(adminService.orderDetail(orderNo));
    }

    @GetMapping("/orders/{orderNo}/logs")
    @ApiOperation("订单流转日志（时间轴）")
    public R<List<OrderLogDO>> orderLogs(@PathVariable String orderNo) {
        return R.ok(adminService.orderLogs(orderNo));
    }

    @GetMapping("/orders/{orderNo}/changes")
    @ApiOperation("改签历史")
    public R<List<OrderChangeDO>> orderChanges(@PathVariable String orderNo) {
        return R.ok(adminService.orderChanges(orderNo));
    }

    // ==================== 区间票 / 售卖窗口 ====================

    @PostMapping("/trains/{id}/sale-window")
    @ApiOperation("设置车次售卖时间窗口（格式 yyyy-MM-dd HH:mm:ss，传空表示不限时）")
    public R<Boolean> updateSaleWindow(@PathVariable Long id,
                                       @RequestParam(required = false) String start,
                                       @RequestParam(required = false) String end) {
        adminService.updateSaleWindow(id, parseTime(start), parseTime(end));
        return R.ok(true);
    }

    @PostMapping("/trains/{id}/segment/init")
    @ApiOperation("初始化车次的区间库存（相邻站单段，并把段余票预热到 Redis）")
    public R<Integer> initSegmentStock(@PathVariable Long id) {
        return R.ok(segmentStockService.initSegments(id));
    }

    @GetMapping("/trains/{id}/segments")
    @ApiOperation("车次的区间库存与段价：段价之和即该区间票价")
    public R<List<TrainSegmentStockDO>> listSegments(@PathVariable Long id) {
        return R.ok(segmentStockService.listSegments(id));
    }

    @PostMapping("/trains/{id}/segment/price")
    @ApiOperation("维护区间段价：body = [{seatType, segIndex, price}]，price 传 null 表示清空后按里程自动折算")
    public R<Integer> updateSegmentPrice(@PathVariable Long id,
                                         @RequestBody List<TrainSegmentStockDO> segments) {
        int rows = 0;
        for (TrainSegmentStockDO seg : segments) {
            if (seg == null || seg.getSeatType() == null || seg.getSegIndex() == null) {
                continue;
            }
            rows += segmentStockService.updateSegmentPrice(id, seg.getSeatType(), seg.getSegIndex(), seg.getPrice());
        }
        return R.ok(rows);
    }

    private java.time.LocalDateTime parseTime(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return java.time.LocalDateTime.parse(text.trim().replace(' ', 'T'));
        } catch (Exception e) {
            throw new BizException(ResultCode.BAD_REQUEST, "时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    // ==================== 风控 / 限流 ====================

    @GetMapping("/risk/events")
    @ApiOperation("风控事件（同一 IP 多账号、极短耗时请求等命中记录）")
    public R<List<String>> riskEvents(@RequestParam(required = false, defaultValue = "50") Integer limit) {
        return R.ok(riskControlService.recentEvents(limit == null ? 50 : limit));
    }

    @GetMapping("/risk/blacklist")
    @ApiOperation("IP 黑名单（IP -> 拉黑原因与解封时间）")
    public R<Map<String, String>> blacklist() {
        return R.ok(rateLimitService.blackIpDetails());
    }

    @PostMapping("/risk/blacklist")
    @ApiOperation("拉黑 IP（不传 seconds 默认 30 分钟）")
    public R<Boolean> blockIp(@RequestParam String ip,
                              @RequestParam(required = false) Long seconds,
                              @RequestParam(required = false) String reason) {
        rateLimitService.blockIp(ip, seconds == null ? 1800L : seconds,
                StringUtils.hasText(reason) ? reason : "管理员手动拉黑");
        return R.ok(true);
    }

    @DeleteMapping("/risk/blacklist/{ip}")
    @ApiOperation("解除 IP 拉黑")
    public R<Boolean> unblockIp(@PathVariable String ip) {
        rateLimitService.unblockIp(ip);
        riskControlService.resetHits(ip);
        return R.ok(true);
    }

    @GetMapping("/seckill/flows")
    @ApiOperation("抢票流水：谁在什么时候抢了哪趟车、结果 / 耗时 / 失败原因（排查超卖与抢票失败用）")
    public R<PageResult<SeckillFlowVO>> seckillFlows(@RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) Long trainId,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                                     @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.seckillFlows(userId, trainId, status, pageNum, pageSize));
    }

    @PostMapping("/orders/{orderNo}/refund")
    @ApiOperation("后台人工退票（客服）")
    public R<Boolean> refundOrder(@PathVariable String orderNo, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        afterSaleService.refund(orderNo, null, "admin", reason);
        return R.ok(true);
    }

    // ==================== 用户 ====================

    @GetMapping("/users")
    @ApiOperation("用户列表（手机号 / 账号模糊搜索）")
    public R<PageResult<UserDO>> users(@RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "1") Integer pageNum,
                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.users(keyword, pageNum, pageSize));
    }

    @PutMapping("/users/{id}/status")
    @ApiOperation("封禁 / 解封用户（封禁会同步踢下线）")
    public R<Boolean> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(id, status);
        return R.ok(true);
    }

    @PostMapping("/users/{id}/kick")
    @ApiOperation("强制下线（拉黑该用户所有 token，立即生效）")
    public R<Integer> kickUser(@PathVariable Long id) {
        return R.ok(adminService.kickUser(id));
    }

    // ==================== 公告 ====================

    @GetMapping("/announcements")
    @ApiOperation("公告列表")
    public R<PageResult<AnnouncementDO>> announcements(@RequestParam(defaultValue = "1") Integer pageNum,
                                                       @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.announcements(pageNum, pageSize));
    }

    @PostMapping("/announcements")
    @ApiOperation("新增 / 编辑公告")
    public R<AnnouncementDO> saveAnnouncement(@RequestBody AnnouncementDO announcement) {
        return R.ok(adminService.saveAnnouncement(announcement));
    }

    @DeleteMapping("/announcements/{id}")
    @ApiOperation("删除公告")
    public R<Boolean> deleteAnnouncement(@PathVariable Long id) {
        adminService.deleteAnnouncement(id);
        return R.ok(true);
    }

    // ==================== 票务监控 ====================

    @GetMapping("/monitor/stock")
    @ApiOperation("余票监控：各车次各席别剩余座位（分页）")
    public R<PageResult<Map<String, Object>>> stockMonitor(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String trainNo) {
        return R.ok(adminService.stockMonitor(pageNum, pageSize, trainNo));
    }

    @GetMapping("/monitor/locked-seats")
    @ApiOperation("锁票管理：临时锁定的座位")
    public R<List<SeatDO>> lockedSeats(@RequestParam(defaultValue = "50") Integer limit) {
        return R.ok(adminService.lockedSeats(limit));
    }

    // ==================== 统计报表 ====================

    @GetMapping("/stats")
    @ApiOperation("运营概览：营收 / 退票 / 用户")
    public R<Map<String, Object>> stats() {
        return R.ok(adminService.stats());
    }

    @GetMapping("/stats/daily-sales")
    @ApiOperation("每日售票统计")
    public R<List<Map<String, Object>>> dailySales() {
        return R.ok(adminService.dailySales());
    }

    @GetMapping("/stats/train-sales")
    @ApiOperation("车次售票统计")
    public R<List<Map<String, Object>>> trainSales() {
        return R.ok(adminService.trainSales());
    }

    @GetMapping("/stats/user-growth")
    @ApiOperation("新增用户统计")
    public R<List<Map<String, Object>>> userGrowth() {
        return R.ok(adminService.userGrowth());
    }

    @GetMapping("/stats/export")
    @ApiOperation("导出报表（CSV，Excel 可直接打开）")
    public void export(@RequestParam(defaultValue = "daily-sales") String type,
                       HttpServletResponse response) throws IOException {
        List<Map<String, Object>> rows;
        String[] headers;
        switch (type) {
            case "train-sales":
                rows = adminService.trainSales();
                headers = new String[]{"车次", "发车日期", "售出数量", "销售额"};
                break;
            case "user-growth":
                rows = adminService.userGrowth();
                headers = new String[]{"日期", "新增用户"};
                break;
            default:
                rows = adminService.dailySales();
                headers = new String[]{"日期", "订单数", "销售额"};
                break;
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + type + ".csv\"");
        // BOM：保证 Excel 打开中文不乱码
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
        writer.println(String.join(",", headers));
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                writer.println(row.values().stream()
                        .map(v -> v == null ? "" : String.valueOf(v))
                        .reduce((a, b) -> a + "," + b).orElse(""));
            }
        }
        writer.flush();
    }
}

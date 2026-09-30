package com.qiangpiao.service.impl;

import com.qiangpiao.bo.RangeBO;
import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.OrderAction;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.ChangeFeeUtil;
import com.qiangpiao.common.util.RefundFeeUtil;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.mapper.AdminMapper;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.service.AfterSaleService;
import com.qiangpiao.service.OrderLogService;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.service.SegmentStockService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.service.WalletService;
import com.qiangpiao.vo.AfterSalePreviewVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 售后服务实现：退票 / 改签 / 售后记录。
 *
 * <pre>
 *   退票：按距开车时间阶梯收手续费（8 天以上免费 / 48h~8天 5% / 24h~48h 10% / 不足 24h 20%）
 *        - 尾数以 5 角为单位，最低 2 元，票价不足 2 元按票价收；
 *        - 改签过的票按「最初购票车次」的开车时间取档（原票不足 8 天，改签到 8 天后仍收 5%）；
 *        - 改签后乘车日期落在春运时段 → 不低于 20%；
 *        - 开车后：改签过的票不可退；未改签的当日 24 点前收 50%，次日及以后不再办理；
 *        - 幂等：Redis 锁 + 订单状态 CAS + 钱包退款幂等键，重复提交只生效一次。
 *
 *   改签：换到新车次同席别，释放原座 → 原子选新座 → 差额多退少补 → 写改签记录
 *        - 一张车票只能改签 1 次（order.changed 标记 + 改签记录双重校验）；
 *        - 改签费按「新旧两张票里较低票价」为基数，费率见 {@link ChangeFeeUtil}；
 *        - 净额 = 票价差 + 改签费：正数补收（补差价连同改签费），负数退还（退差额扣改签费）。
 * </pre>
 * 全流程把订单号写入 MDC，日志可用 traceId / orderNo 串联。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /** 售后操作锁时长（秒） */
    private static final long LOCK_SECONDS = 30;

    private final OrderMapper orderMapper;
    private final AdminMapper adminMapper;
    private final TrainService trainService;
    private final SeatService seatService;
    private final SegmentStockService segmentStockService;
    private final WalletService walletService;
    private final StringRedisTemplate stringRedisTemplate;
    private final OrderLogService orderLogService;

    /** 春运起止：改签后车票乘车日期落在区间内，退票一律 20% */
    @Value("${ticket.spring-festival-start:}")
    private String springFestivalStart;
    @Value("${ticket.spring-festival-end:}")
    private String springFestivalEnd;

    // ==================== 退票 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, Long userId, String operator, String reason) {
        TraceContext.putOrder(orderNo);
        String lockKey = RedisKeys.lock("after-sale:refund:" + orderNo);
        if (!tryLock(lockKey)) {
            throw new BizException(ResultCode.REFUND_REPEAT);
        }
        try {
            OrderDO order = loadPaidOrder(orderNo, userId);
            LocalDateTime now = LocalDateTime.now();
            TrainBO train = trainService.getTrainBO(order.getTrainId());
            // 开车后当日 24 点前 50%、次日及以后不可退、改签过的票开车后不可退
            BigDecimal rate = resolveRefundRate(order, train, now);
            BigDecimal price = nvl(order.getPrice());
            BigDecimal fee = RefundFeeUtil.feeByRate(price, rate);
            BigDecimal refundAmount = price.subtract(fee).max(BigDecimal.ZERO);

            // 状态机 CAS：只有「已支付」能变「已退票」，重复退票在此失败（幂等核心）
            if (orderMapper.updateStatus(orderNo, Constants.ORDER_STATUS_REFUNDED,
                    Constants.ORDER_STATUS_PAID) <= 0) {
                throw new BizException(ResultCode.ORDER_STATUS_ERROR);
            }
            adminMapper.updateOrderRefund(orderNo, fee, refundAmount);

            // 票款退回用户钱包（幂等键：同一订单只入账一次）
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletService.refundOnce(order.getUserId(), refundAmount, refundKey(orderNo), orderNo,
                        "退票 " + train.getTrainNo(),
                        "订单 " + orderNo + " 退票，手续费 " + fee.toPlainString() + " 元");
            }
            // 手续费归平台
            if (fee.compareTo(BigDecimal.ZERO) > 0) {
                creditFeeToPlatform(fee, orderNo, train.getTrainNo());
            }

            // 释放座位 + 归还库存（releaseSeat 按订单号幂等；restoreStock 有可用数上限保护）
            releaseInventory(order);
            stringRedisTemplate.delete(RedisKeys.seckillUser(order.getTrainId(), order.getUserId()));

            trainService.evictTrainCache(order.getTrainId());
            seatService.evictSeatCache(order.getTrainId(), order.getSeatType());

            String detail = "票价 " + price.toPlainString() + " 元，手续费 " + fee.toPlainString()
                    + " 元，实退 " + refundAmount.toPlainString() + " 元（" + RefundFeeUtil.rateText(rate) + "）"
                    + (StringUtils.hasText(reason) ? "，原因：" + reason : "");
            orderLogService.log(orderNo, OrderAction.REFUND, detail, operator);
            log.info("退票成功：orderNo={}, userId={}, 票价={}, 手续费={}, 实退={}, operator={}",
                    orderNo, order.getUserId(), price, fee, refundAmount, operator);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public AfterSalePreviewVO previewRefund(String orderNo, Long userId) {
        OrderDO order = loadPaidOrder(orderNo, userId);
        LocalDateTime now = LocalDateTime.now();
        TrainBO train = trainService.getTrainBO(order.getTrainId());
        BigDecimal rate = resolveRefundRate(order, train, now);
        BigDecimal price = nvl(order.getPrice());
        BigDecimal fee = RefundFeeUtil.feeByRate(price, rate);

        AfterSalePreviewVO vo = new AfterSalePreviewVO();
        vo.setOrderNo(orderNo);
        vo.setBaseAmount(price);
        vo.setFee(fee);
        vo.setRefundAmount(price.subtract(fee).max(BigDecimal.ZERO));
        vo.setPayAmount(BigDecimal.ZERO);
        vo.setFeeRule(RefundFeeUtil.rateText(rate));
        vo.setFree(fee.compareTo(BigDecimal.ZERO) == 0);
        vo.setTip(buildRefundTip(order, rate));
        return vo;
    }

    // ==================== 改签 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void change(String orderNo, Long userId, Long newTrainId, Integer seatType, String reason) {
        TraceContext.putOrder(orderNo);
        String lockKey = RedisKeys.lock("after-sale:change:" + orderNo);
        if (!tryLock(lockKey)) {
            throw new BizException(ResultCode.CHANGE_NOT_ALLOWED);
        }
        try {
            OrderDO order = loadPaidOrder(orderNo, userId);
            // 一张车票只能改签一次：改签记录、订单 changed 标记双重判断，防止并发绕过
            if (isChanged(order)) {
                throw new BizException(ResultCode.CHANGE_ONLY_ONCE);
            }
            // 区间票（跨多段）暂不支持改签：分段库存无法与新车次一一对应，退票重买更干净
            if (isSegmentTicket(order)) {
                throw new BizException(ResultCode.CHANGE_SEGMENT_UNSUPPORTED);
            }
            LocalDateTime now = LocalDateTime.now();
            Long oldTrainId = order.getTrainId();
            Integer oldSeatType = order.getSeatType();
            String oldSeatNo = order.getSeatNo();
            BigDecimal oldPrice = nvl(order.getPrice());

            TrainBO oldTrain = trainService.getTrainBO(oldTrainId);
            if (oldTrain == null) {
                throw new BizException(ResultCode.TRAIN_NOT_FOUND);
            }
            // 新车次必须通过售票规则校验（在售 / 未发车 / 预售期 / 未到停售时间）
            trainService.assertTicketSellable(newTrainId);
            TrainBO newTrain = trainService.getTrainBO(newTrainId);
            TrainStockDO stock = adminMapper.stock(newTrainId, seatType);
            if (stock == null || stock.getAvailableCount() == null || stock.getAvailableCount() <= 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }

            // 改签费：以新旧两张票里较低的票价为基数，按改签场景取费率（见 ChangeFeeUtil）
            BigDecimal newPrice = nvl(stock.getPrice());
            BigDecimal diff = newPrice.subtract(oldPrice);
            ChangeFeeUtil.Result feeResult = ChangeFeeUtil.calc(now, oldTrain.departAt(),
                    order.getDepartDate(), newTrain.getDepartDate());
            if (!feeResult.isAllowed()) {
                throw new BizException(ResultCode.CHANGE_EXPIRED);
            }
            BigDecimal changeFee = ChangeFeeUtil.fee(oldPrice, newPrice, feeResult.getRate());
            // 净额 = 票价差 + 改签费：正数补收（补差价连同改签费），负数退还（退差额扣改签费）
            BigDecimal net = diff.add(changeFee);
            BigDecimal payAmount = net.max(BigDecimal.ZERO);
            BigDecimal backAmount = net.min(BigDecimal.ZERO).abs();

            // 先释放原座位，再原子选新座（同一订单号，顺序不能反）
            adminMapper.releaseSeat(orderNo);
            adminMapper.restoreStock(oldTrainId, oldSeatType);
            int occupied = adminMapper.occupySeat(newTrainId, seatType, orderNo);
            if (occupied <= 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
            SeatDO newSeat = adminMapper.seatOfOrder(newTrainId, orderNo);
            adminMapper.deductStock(newTrainId, seatType);

            if (payAmount.compareTo(BigDecimal.ZERO) > 0) {
                walletService.pay(userId, payAmount, orderNo, "改签补差价及改签费 " + newTrain.getTrainNo(), reason);
                creditToPlatformSafe(payAmount, orderNo, newTrain.getTrainNo());
            }
            if (backAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 幂等键带上新车次：同一订单改签到不同车次可各自退差一次
                walletService.refundOnce(userId, backAmount, changeRefundKey(orderNo, newTrainId), orderNo,
                        "改签退差价 " + newTrain.getTrainNo(),
                        "差额 " + diff.abs().toPlainString() + " 元，改签费 " + changeFee.toPlainString() + " 元");
                creditBackPlatform(backAmount, orderNo, newTrain.getTrainNo());
            }

            // 车次快照同步更新，否则改签后订单仍显示旧车次；
            // origin_depart_time 只在首次改签时落定（保存最初购票车次的开车时间，用于后续退票取档）
            adminMapper.updateOrderForChange(orderNo, newTrainId, seatType,
                    newSeat == null ? null : newSeat.getCarriageNo(),
                    newSeat == null ? null : newSeat.getSeatNo(),
                    stock.getPrice(), newTrain.getDepartDate(),
                    newTrain.getTrainNo(), newTrain.getTrainType(),
                    newTrain.getFromStationName(), newTrain.getToStationName(),
                    newTrain.getDepartTime(), newTrain.getArriveTime(),
                    oldTrain.departAt());

            OrderChangeDO change = new OrderChangeDO();
            change.setOrderNo(orderNo);
            change.setNewOrderNo(orderNo);
            change.setUserId(order.getUserId());
            change.setOldTrainId(oldTrainId);
            change.setNewTrainId(newTrainId);
            change.setOldSeatNo(oldSeatNo);
            change.setNewSeatNo(newSeat == null ? null : newSeat.getSeatNo());
            change.setDiffAmount(diff);
            change.setChangeFee(changeFee);
            change.setFeeRule(feeResult.getRuleText());
            change.setReason(reason);
            adminMapper.insertOrderChange(change);

            // 一人一单标记跟着新班次走
            stringRedisTemplate.delete(RedisKeys.seckillUser(oldTrainId, order.getUserId()));
            trainService.evictTrainCache(oldTrainId);
            trainService.evictTrainCache(newTrainId);
            seatService.evictSeatCache(oldTrainId, oldSeatType);
            seatService.evictSeatCache(newTrainId, seatType);

            String detail = "改签至 " + newTrain.getTrainNo() + "，"
                    + (diff.signum() == 0 ? "票价相同"
                    : (diff.signum() > 0 ? "票价高 " + diff.toPlainString() + " 元"
                    : "票价低 " + diff.abs().toPlainString() + " 元"))
                    + "，改签费 " + changeFee.toPlainString() + " 元（" + feeResult.getRuleText() + "）"
                    + (payAmount.compareTo(BigDecimal.ZERO) > 0 ? "，补收 " + payAmount.toPlainString() + " 元" : "")
                    + (backAmount.compareTo(BigDecimal.ZERO) > 0 ? "，退回 " + backAmount.toPlainString() + " 元" : "");
            orderLogService.log(orderNo, OrderAction.CHANGE, detail, String.valueOf(userId));
            log.info("改签成功：orderNo={}, {} -> {}, 差额={}, 改签费={}, 补收={}, 退回={}",
                    orderNo, oldTrainId, newTrainId, diff, changeFee, payAmount, backAmount);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    @Override
    public AfterSalePreviewVO previewChange(String orderNo, Long userId, Long newTrainId, Integer seatType) {
        OrderDO order = loadPaidOrder(orderNo, userId);
        // 一张车票只能改签一次
        if (isChanged(order)) {
            throw new BizException(ResultCode.CHANGE_ONLY_ONCE);
        }
        LocalDateTime now = LocalDateTime.now();
        TrainBO oldTrain = trainService.getTrainBO(order.getTrainId());
        if (oldTrain == null) {
            throw new BizException(ResultCode.TRAIN_NOT_FOUND);
        }
        TrainBO newTrain = trainService.getTrainBO(newTrainId);
        if (newTrain == null) {
            throw new BizException(ResultCode.TRAIN_NOT_FOUND);
        }
        TrainStockDO stock = adminMapper.stock(newTrainId, seatType);
        if (stock == null || stock.getAvailableCount() == null || stock.getAvailableCount() <= 0) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        BigDecimal oldPrice = nvl(order.getPrice());
        BigDecimal newPrice = nvl(stock.getPrice());
        BigDecimal diff = newPrice.subtract(oldPrice);
        ChangeFeeUtil.Result feeResult = ChangeFeeUtil.calc(now, oldTrain.departAt(),
                order.getDepartDate(), newTrain.getDepartDate());
        if (!feeResult.isAllowed()) {
            throw new BizException(ResultCode.CHANGE_EXPIRED);
        }
        BigDecimal changeFee = ChangeFeeUtil.fee(oldPrice, newPrice, feeResult.getRate());
        BigDecimal net = diff.add(changeFee);

        AfterSalePreviewVO vo = new AfterSalePreviewVO();
        vo.setOrderNo(orderNo);
        vo.setBaseAmount(ChangeFeeUtil.base(oldPrice, newPrice));
        vo.setFee(changeFee);
        vo.setPayAmount(net.max(BigDecimal.ZERO));
        vo.setRefundAmount(net.min(BigDecimal.ZERO).abs());
        vo.setFeeRule(feeResult.getRuleText());
        vo.setFree(changeFee.compareTo(BigDecimal.ZERO) == 0);
        vo.setTip("改签费按新旧两张票中较低票价（¥"
                + ChangeFeeUtil.base(oldPrice, newPrice).toPlainString() + "）计算；一张车票只能改签一次");
        return vo;
    }

    // ==================== 记录 ====================

    @Override
    public List<OrderLogDO> timeline(String orderNo) {
        return orderLogService.timeline(orderNo);
    }

    @Override
    public List<OrderChangeDO> changes(String orderNo) {
        return adminMapper.listOrderChanges(orderNo);
    }

    // ==================== private ====================

    private OrderDO loadPaidOrder(String orderNo, Long userId) {
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!Integer.valueOf(Constants.ORDER_STATUS_PAID).equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        return order;
    }

    /** 退票计费基准时间：改签过的票按最初购票车次的开车时间取档，防止「先改签到远期车次再免费退」 */
    private LocalDateTime feeBaseTime(OrderDO order, TrainBO train) {
        if (isChanged(order) && order.getOriginDepartTime() != null) {
            return order.getOriginDepartTime();
        }
        LocalDateTime departAt = train.departAt();
        return departAt != null ? departAt : order.getOriginDepartTime();
    }

    private boolean isChanged(OrderDO order) {
        return Integer.valueOf(1).equals(order.getChanged());
    }

    private long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (to == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.MINUTES.between(from, to);
    }

    /** 改签后车票的乘车日期是否落在春运时段 */
    private boolean inSpringFestival(LocalDate date) {
        if (date == null || !StringUtils.hasText(springFestivalStart) || !StringUtils.hasText(springFestivalEnd)) {
            return false;
        }
        try {
            LocalDate start = LocalDate.parse(springFestivalStart.trim(), DATE_FORMAT);
            LocalDate end = LocalDate.parse(springFestivalEnd.trim(), DATE_FORMAT);
            return !date.isBefore(start) && !date.isAfter(end);
        } catch (Exception e) {
            log.warn("春运时段配置解析失败，按非春运处理：start={}, end={}", springFestivalStart, springFestivalEnd);
            return false;
        }
    }

    /**
     * 退票费率取档（退票与试算共用，保证展示与实扣一致）：
     * <ul>
     *   <li>已发车且改签过 → 不可退（改签后的车票开车后不办理退票）</li>
     *   <li>已发车且未改签 → 当日 24 点前 50%；次日及以后不再办理</li>
     *   <li>未发车 → 按「最初购票车次」开车时间取阶梯档</li>
     *   <li>改签后乘车日期落在春运时段 → 不低于 20%</li>
     * </ul>
     */
    private BigDecimal resolveRefundRate(OrderDO order, TrainBO train, LocalDateTime now) {
        if (departed(order, train, now)) {
            if (isChanged(order)) {
                throw new BizException(ResultCode.REFUND_CHANGED_AFTER_DEPART);
            }
            if (!isDepartDay(order, train, now)) {
                throw new BizException(ResultCode.REFUND_EXPIRED);
            }
            return RefundFeeUtil.RATE_50;
        }
        BigDecimal rate = RefundFeeUtil.rate(minutesBetween(now, feeBaseTime(order, train)));
        if (isChanged(order) && inSpringFestival(order.getDepartDate())) {
            rate = rate.max(RefundFeeUtil.RATE_20);
        }
        return rate;
    }

    /** 是否已发车：优先用车次实时发车时刻，缺失时退回订单记录的原始开车时间 */
    private boolean departed(OrderDO order, TrainBO train, LocalDateTime now) {
        LocalDateTime departAt = train == null ? null : train.departAt();
        if (departAt == null) {
            departAt = feeBaseTime(order, train);
        }
        return departAt == null || !departAt.isAfter(now);
    }

    /** 当前时间是否仍在车票乘车日当天（开车后当日 24 点前） */
    private boolean isDepartDay(OrderDO order, TrainBO train, LocalDateTime now) {
        LocalDate date = order.getDepartDate();
        if (date == null && train != null && train.departAt() != null) {
            date = train.departAt().toLocalDate();
        }
        return date != null && date.equals(now.toLocalDate());
    }

    private String buildRefundTip(OrderDO order, BigDecimal rate) {
        if (rate != null && rate.compareTo(RefundFeeUtil.RATE_50) == 0) {
            return "列车已发车，当日 24 点前退票按 50% 收取手续费，次日及以后不再办理退票";
        }
        if (isChanged(order) && inSpringFestival(order.getDepartDate())) {
            return "该票已改签且乘车日期在春运时段，退票费按 20% 收取";
        }
        if (isChanged(order)) {
            return "已改签的车票按最初购票车次的开车时间计费（原票不足 8 天，改签到 8 天以后再退仍收 5%）"
                    + "；一张车票只能改签一次，改签后开车不可退票";
        }
        return "开车前 8 天以上免收手续费，越接近开车手续费越高；开车后当日 24 点前收 50%，次日不可退";
    }

    /** 释放座位 + 归还库存（幂等：重复执行不会把余票抬超总座数） */
    private void releaseInventory(OrderDO order) {
        // 区间感知的座位释放：只有「该座位所有区间都被释放」才把座位置回可售
        seatService.releaseSeat(order.getOrderNo());
        RangeBO range = rangeOfOrder(order);
        if (segmentStockService.enabled()) {
            // 区间票：归还该区间覆盖的每一段（DB + Redis）
            segmentStockService.release(order.getTrainId(), order.getSeatType(), range, 1);
            segmentStockService.rollbackRedis(order.getTrainId(), order.getSeatType(), range, 1);
        } else {
            adminMapper.restoreStock(order.getTrainId(), order.getSeatType());
            rollbackRedisStock(order.getTrainId(), order.getSeatType());
        }
    }

    /** 订单的乘车区间：落库时已快照站序，缺失时按车次时刻表补解析 */
    private RangeBO rangeOfOrder(OrderDO order) {
        if (order.getFromStopOrder() != null && order.getToStopOrder() != null
                && order.getToStopOrder() > order.getFromStopOrder()) {
            return RangeBO.builder()
                    .fromOrder(order.getFromStopOrder())
                    .toOrder(order.getToStopOrder())
                    .build();
        }
        return segmentStockService.resolveRange(order.getTrainId(), null, null);
    }

    /**
     * 是否区间票（跨多段）。
     * 改签要「释放原座位 → 归还原库存 → 抢新座位 → 扣新库存」，
     * 区间票的库存是分段计数的，改签后新旧区间无法一一对应，因此暂不支持（退票重买）。
     */
    private boolean isSegmentTicket(OrderDO order) {
        if (!segmentStockService.enabled()) {
            return false;
        }
        RangeBO range = rangeOfOrder(order);
        return range.segmentCount() > 1;
    }

    private String refundKey(String orderNo) {
        return "REFUND:" + orderNo;
    }

    private String changeRefundKey(String orderNo, Long newTrainId) {
        return "CHANGE_REFUND:" + orderNo + ":" + newTrainId;
    }

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", LOCK_SECONDS, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // Redis 不可用时退化为数据库 CAS 兜底，不阻断业务
            log.warn("获取售后操作锁失败，降级为数据库 CAS：key={}, msg={}", lockKey, e.getMessage());
            return true;
        }
    }

    private void creditToPlatformSafe(BigDecimal amount, String orderNo, String trainNo) {
        try {
            walletService.creditToPlatform(Constants.PLATFORM_USER_ID, amount, orderNo,
                    "改签补差价收入 " + trainNo, "订单 " + orderNo);
        } catch (Exception e) {
            log.warn("平台账户入账失败（不影响主流程）：orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }

    private void creditBackPlatform(BigDecimal amount, String orderNo, String trainNo) {
        try {
            walletService.pay(Constants.PLATFORM_USER_ID, amount, orderNo,
                    "退票/退差价冲退 " + trainNo, "订单 " + orderNo);
        } catch (Exception e) {
            log.warn("平台账户冲退失败（不影响主流程）：orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }

    private void creditFeeToPlatform(BigDecimal fee, String orderNo, String trainNo) {
        try {
            walletService.creditToPlatform(Constants.PLATFORM_USER_ID, fee, orderNo,
                    "退票手续费 " + trainNo, "订单 " + orderNo + " 退票手续费");
        } catch (Exception e) {
            log.warn("手续费入平台账户失败（不影响主流程）：orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }

    private void rollbackRedisStock(Long trainId, Integer seatType) {
        try {
            String key = RedisKeys.seckillStock(trainId, seatType);
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
                stringRedisTemplate.opsForValue().increment(key);
            }
        } catch (Exception e) {
            log.error("回滚 Redis 库存失败", e);
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }


}

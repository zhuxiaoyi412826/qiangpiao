package com.qiangpiao.service.impl;

import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dataobject.OrderChangeDO;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.mapper.AdminMapper;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.service.AfterSaleService;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 售后服务实现：退票 / 改签 / 售后记录。
 * <pre>
 *   退票：已支付且未发车 -> 释放座位 + 归还库存 + 票款退回用户 + 平台账户冲退
 *   改签：换到新车次同席别 -> 释放原座 -> 原子选新座 -> 差额多退少补 -> 写改签记录
 * </pre>
 * 全流程把订单号写入 MDC，日志可用 traceId / orderNo 串联。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleServiceImpl implements AfterSaleService {

    private final OrderMapper orderMapper;
    private final AdminMapper adminMapper;
    private final TrainService trainService;
    private final SeatService seatService;
    private final WalletService walletService;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(String orderNo, Long userId, String operator, String reason) {
        TraceContext.putOrder(orderNo);
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
        TrainBO train = trainService.getTrainBO(order.getTrainId());
        if (train.departed(LocalDateTime.now())) {
            throw new BizException(ResultCode.TRAIN_DEPARTED);
        }

        BigDecimal price = order.getPrice() == null ? BigDecimal.ZERO : order.getPrice();
        // 1. 票款退回用户钱包
        walletService.refund(order.getUserId(), price, orderNo, "退票 " + train.getTrainNo(), reason);
        // 2. 平台账户冲退（资金闭环反向；失败不影响退票主流程）
        creditBackPlatform(price, orderNo, train.getTrainNo());

        // 3. 订单置为已退票
        adminMapper.updateOrderStatus(orderNo, Constants.ORDER_STATUS_REFUNDED);
        // 4. 释放座位 + 归还库存
        adminMapper.releaseSeat(orderNo);
        adminMapper.restoreStock(order.getTrainId(), order.getSeatType());
        rollbackRedisStock(order.getTrainId(), order.getSeatType());
        stringRedisTemplate.delete(RedisKeys.seckillUser(order.getTrainId(), order.getUserId()));

        // 5. 失效缓存
        trainService.evictTrainCache(order.getTrainId());
        seatService.evictSeatCache(order.getTrainId(), order.getSeatType());

        String detail = "退款 " + price + " 元" + (StringUtils.hasText(reason) ? "，原因：" + reason : "");
        appendLog(orderNo, "REFUND", "退票成功", detail, operator);
        log.info("退票成功：orderNo={}, userId={}, 退款={}, operator={}", orderNo, order.getUserId(), price, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void change(String orderNo, Long userId, Long newTrainId, Integer seatType, String reason) {
        TraceContext.putOrder(orderNo);
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
        Long oldTrainId = order.getTrainId();
        Integer oldSeatType = order.getSeatType();
        String oldSeatNo = order.getSeatNo();
        BigDecimal oldPrice = order.getPrice() == null ? BigDecimal.ZERO : order.getPrice();

        // 新车次必须通过售票规则校验（在售 / 未发车 / 预售期 / 未到停售时间）
        trainService.assertTicketSellable(newTrainId);
        TrainBO newTrain = trainService.getTrainBO(newTrainId);
        TrainStockDO stock = adminMapper.stock(newTrainId, seatType);
        if (stock == null || stock.getAvailableCount() == null || stock.getAvailableCount() <= 0) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        // 先释放原座位，再原子选新座（同一订单号，顺序不能反）
        adminMapper.releaseSeat(orderNo);
        adminMapper.restoreStock(oldTrainId, oldSeatType);
        int occupied = adminMapper.occupySeat(newTrainId, seatType, orderNo);
        if (occupied <= 0) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        SeatDO newSeat = adminMapper.seatOfOrder(newTrainId, orderNo);
        adminMapper.deductStock(newTrainId, seatType);

        // 差额多退少补
        BigDecimal diff = (stock.getPrice() == null ? BigDecimal.ZERO : stock.getPrice()).subtract(oldPrice);
        if (diff.signum() > 0) {
            walletService.pay(userId, diff, orderNo, "改签补差价 " + newTrain.getTrainNo(), reason);
            creditToPlatformSafe(diff, orderNo, newTrain.getTrainNo());
        } else if (diff.signum() < 0) {
            walletService.refund(userId, diff.abs(), orderNo, "改签退差价 " + newTrain.getTrainNo(), reason);
            creditBackPlatform(diff.abs(), orderNo, newTrain.getTrainNo());
        }

        adminMapper.updateOrderForChange(orderNo, newTrainId, seatType,
                newSeat == null ? null : newSeat.getCarriageNo(),
                newSeat == null ? null : newSeat.getSeatNo(),
                stock.getPrice(), newTrain.getDepartDate());

        OrderChangeDO change = new OrderChangeDO();
        change.setOrderNo(orderNo);
        change.setNewOrderNo(orderNo);
        change.setUserId(order.getUserId());
        change.setOldTrainId(oldTrainId);
        change.setNewTrainId(newTrainId);
        change.setOldSeatNo(oldSeatNo);
        change.setNewSeatNo(newSeat == null ? null : newSeat.getSeatNo());
        change.setDiffAmount(diff);
        change.setReason(reason);
        adminMapper.insertOrderChange(change);

        // 一人一单标记跟着新班次走
        stringRedisTemplate.delete(RedisKeys.seckillUser(oldTrainId, order.getUserId()));
        trainService.evictTrainCache(oldTrainId);
        trainService.evictTrainCache(newTrainId);
        seatService.evictSeatCache(oldTrainId, oldSeatType);
        seatService.evictSeatCache(newTrainId, seatType);

        String detail = "改签至 " + newTrain.getTrainNo() + "，" + (diff.signum() == 0 ? "票价相同" : "差额 " + diff + " 元");
        appendLog(orderNo, "CHANGE", "改签成功", detail, String.valueOf(userId));
        log.info("改签成功：orderNo={}, {} -> {}, 差额={}", orderNo, oldTrainId, newTrainId, diff);
    }

    @Override
    public List<OrderLogDO> timeline(String orderNo) {
        return adminMapper.listOrderLogs(orderNo);
    }

    @Override
    public List<OrderChangeDO> changes(String orderNo) {
        return adminMapper.listOrderChanges(orderNo);
    }

    // ==================== private ====================

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

    /** 写入订单流转日志（时间轴节点），失败不影响主流程 */
    private void appendLog(String orderNo, String action, String actionText, String detail, String operator) {
        try {
            OrderLogDO logDO = new OrderLogDO();
            logDO.setOrderNo(orderNo);
            logDO.setAction(action);
            logDO.setActionText(actionText);
            logDO.setDetail(detail);
            logDO.setOperator(operator);
            logDO.setTraceId(TraceContext.traceId());
            adminMapper.insertOrderLog(logDO);
        } catch (Exception e) {
            log.warn("写入订单流转日志失败：orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }
}

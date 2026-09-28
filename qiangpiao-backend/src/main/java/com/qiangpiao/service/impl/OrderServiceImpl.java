package com.qiangpiao.service.impl;

import com.qiangpiao.bo.OrderBO;
import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.bo.SeatBO;
import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.dataobject.SeckillRecordDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.dto.OrderQueryDTO;
import com.qiangpiao.dto.PayDTO;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.mapper.AdminMapper;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.mapper.SeckillRecordMapper;
import com.qiangpiao.service.PaymentService;
import com.qiangpiao.service.PurchaseLimitService;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.OrderService;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.OrderDetailVO;
import com.qiangpiao.vo.OrderVO;
import com.qiangpiao.vo.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现：下单 / 支付 / 取消 / 超时关单 / 秒杀异步落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /** 乐观锁重试次数 */
    private static final int RETRY_TIMES = 3;

    private final OrderMapper orderMapper;
    private final AdminMapper adminMapper;
    private final TrainStockMapper trainStockMapper;
    private final SeckillRecordMapper seckillRecordMapper;
    private final TrainService trainService;
    private final SeatService seatService;
    private final PaymentService paymentService;
    private final PurchaseLimitService purchaseLimitService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${order.pay-timeout-minutes}")
    private int payTimeoutMinutes;

    @Override
    public PageResult<OrderVO> page(Long userId, OrderQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();
        long offset = (long) (pageNum - 1) * pageSize;

        List<OrderDO> orders = orderMapper.selectByUserId(userId, queryDTO.getStatus(), offset, (long) pageSize);
        long total = orderMapper.countByUserId(userId, queryDTO.getStatus());
        List<OrderVO> list = orders.stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(pageNum, pageSize, total, list);
    }

    @Override
    public OrderDetailVO detail(String orderNo, Long userId) {
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        assertOrderOwner(order, userId);
        TrainBO train = resolveTrain(order);
        // 购票当天把乘车日期快照到订单，避免车次日期滚动导致历史订单日期被改写
        LocalDate snapshotDate = order.getDepartDate() != null ? order.getDepartDate() : train.getDepartDate();
        return OrderDetailVO.builder()
                .orderNo(order.getOrderNo())
                .trainNo(train.getTrainNo())
                .fromStationName(train.getFromStationName())
                .toStationName(train.getToStationName())
                .departDate(snapshotDate == null ? null : snapshotDate.toString())
                .departTime(train.getDepartTime() == null ? null : train.getDepartTime().toString())
                .arriveTime(train.getArriveTime() == null ? null : train.getArriveTime().toString())
                .seatTypeName(TrainServiceImpl.seatTypeName(order.getSeatType()))
                .carriageNo(order.getCarriageNo())
                .seatNo(order.getSeatNo())
                .passengerName(order.getPassengerName())
                .idCard(maskIdCard(order.getIdCard()))
                .price(order.getPrice())
                .status(order.getStatus())
                .statusText(statusText(order.getStatus()))
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .expireTime(order.getExpireTime())
                .build();
    }

    @Override
    public PaymentVO pay(PayDTO payDTO, Long userId) {
        // 只负责「发起支付」：建支付单 + 等渠道回调，扣款/置已支付在回调里做（PaymentServiceImpl）
        return paymentService.createPayment(payDTO.getOrderNo(), userId,
                payDTO.getPayType(), payDTO.getIdempotentKey());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo, Long userId) {
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        assertOrderOwner(order, userId);
        if (order.getStatus() == null || order.getStatus() != Constants.ORDER_STATUS_WAIT_PAY) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        int rows = orderMapper.markCancelled(orderNo, LocalDateTime.now());
        if (rows <= 0) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        // 关闭名下未终态的支付单，后续回调命中幂等不会误入账
        paymentService.closeByOrderNo(orderNo, "订单已取消");
        // 释放座位
        seatService.releaseSeat(orderNo);
        // 归还 DB 库存
        TrainStockDO stock = trainStockMapper.selectByTrainAndType(order.getTrainId(), order.getSeatType());
        if (stock != null) {
            trainStockMapper.increaseStock(stock.getId());
        }
        // 归还 Redis 库存 + 清理一人一单标记
        rollbackRedisStock(order.getTrainId(), order.getSeatType());
        stringRedisTemplate.delete(RedisKeys.seckillUser(order.getTrainId(), order.getUserId()));
        stringRedisTemplate.delete(RedisKeys.seckillResult(order.getTrainId(), order.getSeatType(), order.getUserId()));
        seckillRecordMapper.deleteByOrderNo(orderNo);

        // 失效缓存，保证余票展示一致
        trainService.evictTrainCache(order.getTrainId());
        seatService.evictSeatCache(order.getTrainId(), order.getSeatType());
        appendOrderLog(orderNo, "CANCEL", "取消订单", "座位已释放，库存已归还", String.valueOf(order.getUserId()));
        log.info("订单取消成功：orderNo={}, userId={}", orderNo, userId);
    }

    @Override
    public OrderBO getOrderBO(String orderNo) {
        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        TrainBO train = resolveTrain(order);
        return OrderBO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .trainId(order.getTrainId())
                .seatId(order.getSeatId())
                .seatType(order.getSeatType())
                .carriageNo(order.getCarriageNo())
                .seatNo(order.getSeatNo())
                .passengerName(order.getPassengerName())
                .idCard(order.getIdCard())
                .price(order.getPrice())
                .status(order.getStatus())
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .expireTime(order.getExpireTime())
                .trainNo(train.getTrainNo())
                .fromStationName(train.getFromStationName())
                .toStationName(train.getToStationName())
                .departTime(train.getDepartDate() == null || train.getDepartTime() == null
                        ? null : LocalDateTime.of(train.getDepartDate(), train.getDepartTime()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDO createSeckillOrder(SeckillTaskBO taskBO) {
        // 0. 限购兜底：并发下同步层可能同时放行，落库前以 DB 为准再判一次
        //    （每人每天每车次 1 张 + 已购车次运行时间内不可再买）
        TrainBO trainBO = trainService.getTrainBO(taskBO.getTrainId());
        purchaseLimitService.assertCanBuy(taskBO.getUserId(), trainBO);

        // 1. 幂等：一人一单（唯一索引兜底）
        SeckillRecordDO record = new SeckillRecordDO();
        record.setTrainId(taskBO.getTrainId());
        record.setSeatType(taskBO.getSeatType());
        record.setUserId(taskBO.getUserId());
        record.setOrderNo(taskBO.getOrderNo());
        if (seckillRecordMapper.insertIgnore(record) <= 0) {
            throw new BizException(ResultCode.SECKILL_REPEAT);
        }

        // 2. 乐观锁扣减 DB 库存（防超卖最后一道防线）
        TrainStockDO stock = trainStockMapper.selectByTrainAndType(taskBO.getTrainId(), taskBO.getSeatType());
        if (stock == null) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        boolean decreased = false;
        for (int i = 0; i < RETRY_TIMES && !decreased; i++) {
            if (stock.getAvailableCount() == null || stock.getAvailableCount() <= 0) {
                break;
            }
            decreased = trainStockMapper.decreaseStock(stock.getId(), stock.getVersion()) > 0;
            if (!decreased) {
                stock = trainStockMapper.selectByTrainAndType(taskBO.getTrainId(), taskBO.getSeatType());
            }
        }
        if (!decreased) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        // 3. 抢占座位
        SeatBO seatBO = seatService.pickAndLockSeat(taskBO.getTrainId(), taskBO.getSeatType(),
                taskBO.getSeatId(), taskBO.getOrderNo());

        // 4. 创建订单（待支付）
        OrderDO order = new OrderDO();
        order.setOrderNo(taskBO.getOrderNo());
        order.setUserId(taskBO.getUserId());
        order.setTrainId(taskBO.getTrainId());
        // 车次快照：车次日期会按天滚动、也可能被清理，历史订单必须自带车次信息
        order.setTrainNoSnapshot(trainBO.getTrainNo());
        order.setTrainTypeSnapshot(trainBO.getTrainType());
        order.setFromStationSnapshot(trainBO.getFromStationName());
        order.setToStationSnapshot(trainBO.getToStationName());
        order.setDepartTimeSnapshot(trainBO.getDepartTime());
        order.setArriveTimeSnapshot(trainBO.getArriveTime());
        order.setSeatId(seatBO.getSeatId());
        order.setSeatType(taskBO.getSeatType());
        order.setCarriageNo(seatBO.getCarriageNo());
        order.setSeatNo(seatBO.getSeatNo());
        order.setPassengerName(taskBO.getPassengerName());
        order.setIdCard(taskBO.getIdCard());
        order.setPrice(stock.getPrice());
        // 乘车日期快照：后续车次日期滚动时，已生成订单的乘车日期保持不变
        order.setDepartDate(trainBO.getDepartDate());
        order.setStatus(Constants.ORDER_STATUS_WAIT_PAY);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        orderMapper.insert(order);

        // 5. 失效缓存
        trainService.evictTrainCache(taskBO.getTrainId());
        seatService.evictSeatCache(taskBO.getTrainId(), taskBO.getSeatType());
        appendOrderLog(order.getOrderNo(), "CREATE", "下单成功",
                "车次 " + taskBO.getTrainId() + " " + order.getCarriageNo() + "车" + order.getSeatNo() + "座，待支付 " + order.getPrice() + " 元",
                String.valueOf(order.getUserId()));
        log.info("秒杀订单落库成功：orderNo={}, userId={}, trainId={}, seat={}车{}座",
                taskBO.getOrderNo(), taskBO.getUserId(), taskBO.getTrainId(),
                seatBO.getCarriageNo(), seatBO.getSeatNo());
        return order;
    }

    @Override
    public int closeExpiredOrders() {
        List<OrderDO> expired = orderMapper.selectExpiredOrders(LocalDateTime.now(), 200L);
        int count = 0;
        for (OrderDO order : expired) {
            try {
                // 走代理调用，保证 cancel 的事务生效（@EnableAspectJAutoProxy(exposeProxy = true)）
                ((OrderService) org.springframework.aop.framework.AopContext.currentProxy())
                        .cancel(order.getOrderNo(), order.getUserId());
                count++;
            } catch (Exception e) {
                log.warn("超时关单失败：orderNo={}, msg={}", order.getOrderNo(), e.getMessage());
                orderMapper.updateStatus(order.getOrderNo(), Constants.ORDER_STATUS_EXPIRED,
                        Constants.ORDER_STATUS_WAIT_PAY);
                paymentService.closeByOrderNo(order.getOrderNo(), "订单已超时");
            }
        }
        if (count > 0) {
            log.info("超时未支付订单自动关闭：{} 笔", count);
        }
        return count;
    }

    // ==================== private ====================

    /**
     * 取订单的车次信息：优先用订单快照，其次才查 t_train。
     * 车次会按天滚动、也可能被重建清理，历史订单不能因为车次不在就整页报错或消失。
     */
    private TrainBO resolveTrain(OrderDO order) {
        TrainBO train = null;
        try {
            train = trainService.getTrainBO(order.getTrainId());
        } catch (Exception e) {
            log.debug("车次已下线，改用订单快照：orderNo={}, trainId={}, msg={}",
                    order.getOrderNo(), order.getTrainId(), e.getMessage());
        }
        if (order.getTrainNoSnapshot() != null) {
            TrainBO snapshot = train == null ? new TrainBO() : train;
            snapshot.setTrainNo(order.getTrainNoSnapshot());
            snapshot.setTrainType(order.getTrainTypeSnapshot());
            snapshot.setFromStationName(order.getFromStationSnapshot());
            snapshot.setToStationName(order.getToStationSnapshot());
            if (order.getDepartTimeSnapshot() != null) {
                snapshot.setDepartTime(order.getDepartTimeSnapshot());
            }
            if (order.getArriveTimeSnapshot() != null) {
                snapshot.setArriveTime(order.getArriveTimeSnapshot());
            }
            if (order.getDepartDate() != null) {
                snapshot.setDepartDate(order.getDepartDate());
            }
            return snapshot;
        }
        if (train != null) {
            return train;
        }
        // 老订单既没快照、车次也没了：兜底展示，避免订单列表 / 车票列表整页报错
        TrainBO fallback = new TrainBO();
        fallback.setTrainNo("已下线车次");
        fallback.setFromStationName("-");
        fallback.setToStationName("-");
        fallback.setDepartDate(order.getDepartDate());
        return fallback;
    }

    private void assertOrderOwner(OrderDO order, Long userId) {
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN);
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

    private OrderVO toVO(OrderDO order) {
        TrainBO train = resolveTrain(order);
        LocalDate snapshotDate = order.getDepartDate() != null ? order.getDepartDate() : train.getDepartDate();
        return OrderVO.builder()
                .id(order.getId())
                .orderNo(order.getOrderNo())
                .trainNo(train.getTrainNo())
                .fromStationName(train.getFromStationName())
                .toStationName(train.getToStationName())
                .departTime(snapshotDate == null || train.getDepartTime() == null
                        ? null : LocalDateTime.of(snapshotDate, train.getDepartTime()))
                .seatType(order.getSeatType())
                .seatTypeName(TrainServiceImpl.seatTypeName(order.getSeatType()))
                .carriageNo(order.getCarriageNo())
                .seatNo(order.getSeatNo())
                .passengerName(order.getPassengerName())
                .price(order.getPrice())
                .status(order.getStatus())
                .statusText(statusText(order.getStatus()))
                .createTime(order.getCreateTime())
                .expireTime(order.getExpireTime())
                .build();
    }

    public static String statusText(Integer status) {
        if (status == null) {
            return "未知";
        }
        switch (status) {
            case Constants.ORDER_STATUS_WAIT_PAY:
                return "待支付";
            case Constants.ORDER_STATUS_PAID:
                return "已支付";
            case Constants.ORDER_STATUS_CANCELLED:
                return "已取消";
            case Constants.ORDER_STATUS_REFUNDED:
                return "已退票";
            case Constants.ORDER_STATUS_EXPIRED:
                return "已超时";
            default:
                return "未知";
        }
    }

    /**
     * 写入订单流转日志（时间轴节点）：下单 / 支付 / 取消 / 退票 / 改签 / 超时。
     * 日志写入失败不影响主流程。
     */
    private void appendOrderLog(String orderNo, String action, String actionText, String detail, String operator) {
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

    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "********" + idCard.substring(idCard.length() - 4);
    }
}

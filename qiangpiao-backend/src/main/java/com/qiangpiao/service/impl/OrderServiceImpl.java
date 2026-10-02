package com.qiangpiao.service.impl;

import com.qiangpiao.bo.OrderBO;
import com.qiangpiao.bo.RangeBO;
import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.bo.SeatBO;
import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.OrderAction;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.SensitiveCrypto;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.dataobject.SeckillRecordDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.dto.OrderQueryDTO;
import com.qiangpiao.dto.PayDTO;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.mapper.SeckillRecordMapper;
import com.qiangpiao.service.OrderLogService;
import com.qiangpiao.service.PaymentService;
import com.qiangpiao.service.PurchaseLimitService;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.OrderService;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.service.SegmentStockService;
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
import java.util.Collections;
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
    private final TrainStockMapper trainStockMapper;
    private final SeckillRecordMapper seckillRecordMapper;
    private final TrainService trainService;
    private final SeatService seatService;
    private final SegmentStockService segmentStockService;
    private final PaymentService paymentService;
    private final PurchaseLimitService purchaseLimitService;
    private final StringRedisTemplate stringRedisTemplate;
    private final SensitiveCrypto crypto;
    private final OrderLogService orderLogService;

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
                // 库里是密文：解密后脱敏出接口
                .idCard(crypto.maskIdCard(order.getIdCard()))
                .price(order.getPrice())
                .status(order.getStatus())
                .statusText(statusText(order.getStatus()))
                .createTime(order.getCreateTime())
                .payTime(order.getPayTime())
                .expireTime(order.getExpireTime())
                .refundFee(order.getRefundFee())
                .refundAmount(order.getRefundAmount())
                .changed(order.getChanged())
                .refundRuleTip("开车前8天以上免费；48小时~8天收5%；24~48小时收10%；不足24小时收20%；"
                        + "发车后当日24点前收50%，次日及以后不再办理退票（尾数5角取整，最低2元）")
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
        cancelInternal(orderNo, userId, OrderAction.CANCEL, "用户主动取消，座位已释放，库存已归还");
        log.info("订单取消成功：orderNo={}, userId={}", orderNo, userId);
    }

    /**
     * 带动作区分的关单：用户主动取消记 CANCEL，超时被系统关闭记 EXPIRE。
     * 单独暴露到接口是为了让定时任务走 AOP 代理调用，保证事务生效。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(String orderNo, Long userId, OrderAction action, String detail) {
        cancelInternal(orderNo, userId, action, detail);
    }

    private void cancelInternal(String orderNo, Long userId, OrderAction action, String detail) {
        TraceContext.putOrder(orderNo);
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
        // 归还区间库存（区间票：该区间覆盖的每一段都 +1）
        segmentStockService.release(order.getTrainId(), order.getSeatType(), rangeOfOrder(order), 1);
        // 归还 Redis 库存 + 释放 1 张购票额度（批量下单时只释放这一张，不影响同批次其它票）
        rollbackRedisStock(order.getTrainId(), order.getSeatType());
        releaseQuota(order.getTrainId(), order.getUserId());
        stringRedisTemplate.delete(RedisKeys.seckillResult(order.getTrainId(), order.getSeatType(), order.getUserId()));
        stringRedisTemplate.delete(RedisKeys.seckillTicket(order.getOrderNo()));
        seckillRecordMapper.deleteByOrderNo(orderNo);

        // 失效缓存，保证余票展示一致
        trainService.evictTrainCache(order.getTrainId());
        seatService.evictSeatCache(order.getTrainId(), order.getSeatType());
        orderLogService.log(orderNo, action, detail, String.valueOf(order.getUserId()));
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
                // 内部流转用明文（支付 / 票据），不出接口
                .idCard(crypto.decrypt(order.getIdCard()))
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
        //    （同一车次最多 9 张 + 同一乘车人不可重复 + 已购车次运行时间内不可再买）
        TrainBO trainBO = trainService.getTrainBO(taskBO.getTrainId());
        String idCardCipher = crypto.encrypt(taskBO.getIdCard());
        purchaseLimitService.assertCanBuyBatch(taskBO.getUserId(), trainBO, 1,
                Collections.singletonList(idCardCipher));

        // 1. 幂等：同一用户 + 车次 + 席别 + 乘车人只能有一张（唯一索引兜底）
        SeckillRecordDO record = new SeckillRecordDO();
        record.setTrainId(taskBO.getTrainId());
        record.setSeatType(taskBO.getSeatType());
        record.setUserId(taskBO.getUserId());
        record.setOrderNo(taskBO.getOrderNo());
        record.setIdCard(idCardCipher);
        if (seckillRecordMapper.insertIgnore(record) <= 0) {
            throw new BizException(ResultCode.SECKILL_REPEAT);
        }

        // 2. 库存：区间票模式扣「覆盖的每一段」，否则扣全程库存（防超卖最后一道防线）
        //    条件更新代替「select + version 乐观锁重试」：批量并发下旧写法会因重试次数耗尽
        //    把还有票的情况误判成「余票不足」，这里一条语句即可，靠行锁串行且不超卖。
        RangeBO range = rangeOf(taskBO);
        boolean segmentMode = segmentStockService.enabled()
                && segmentStockService.available(taskBO.getTrainId(), taskBO.getSeatType(), range) != null;
        TrainStockDO stock = trainStockMapper.selectByTrainAndType(taskBO.getTrainId(), taskBO.getSeatType());
        if (stock == null) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        if (segmentMode) {
            if (!segmentStockService.occupy(taskBO.getTrainId(), taskBO.getSeatType(), range, 1)) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
        } else {
            int decreased = 0;
            for (int i = 0; i < RETRY_TIMES && decreased <= 0; i++) {
                decreased = trainStockMapper.decreaseStockByTrain(taskBO.getTrainId(), taskBO.getSeatType());
            }
            if (decreased <= 0) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
        }

        // 3. 抢占座位（同批次优先分配同一车厢，坐不下再分其它车厢）
        SeatBO seatBO = seatService.pickAndLockSeat(taskBO.getTrainId(), taskBO.getSeatType(),
                taskBO.getSeatId(), taskBO.getOrderNo(), taskBO.getPreferCarriageNo(), range);

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
        // 区间票：实际乘车区间（站序 + 站名），全程票时为始发 / 终到
        order.setFromStopOrder(range.getFromOrder());
        order.setToStopOrder(range.getToOrder());
        if (org.springframework.util.StringUtils.hasText(range.getFromStationName())) {
            order.setFromStationSnapshot(range.getFromStationName());
        }
        if (org.springframework.util.StringUtils.hasText(range.getToStationName())) {
            order.setToStationSnapshot(range.getToStationName());
        }
        order.setDepartTimeSnapshot(trainBO.getDepartTime());
        order.setArriveTimeSnapshot(trainBO.getArriveTime());
        order.setSeatId(seatBO.getSeatId());
        order.setSeatType(taskBO.getSeatType());
        order.setCarriageNo(seatBO.getCarriageNo());
        order.setSeatNo(seatBO.getSeatNo());
        order.setPassengerName(taskBO.getPassengerName());
        // 身份证明文进，密文落库
        order.setIdCard(idCardCipher);
        // 分段计价：区间票按「覆盖各段的段价之和」收费；段价未维护时按里程比例折算，都缺才退回全程价
        order.setPrice(segmentStockService.fare(taskBO.getTrainId(), taskBO.getSeatType(), range,
                stock.getPrice()));
        // 乘车日期快照：后续车次日期滚动时，已生成订单的乘车日期保持不变
        order.setDepartDate(trainBO.getDepartDate());
        // 最初购票车次的开车时间：改签后不更新，退票费率按这个时间取档
        order.setOriginDepartTime(trainBO.departAt());
        order.setStatus(Constants.ORDER_STATUS_WAIT_PAY);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        orderMapper.insert(order);

        // 5. 失效缓存
        trainService.evictTrainCache(taskBO.getTrainId());
        seatService.evictSeatCache(taskBO.getTrainId(), taskBO.getSeatType());
        orderLogService.log(order.getOrderNo(), OrderAction.CREATE,
                "车次 " + trainBO.getTrainNo() + "，" + order.getCarriageNo() + "车" + order.getSeatNo()
                        + "座，票价 " + order.getPrice() + " 元，请在 " + payTimeoutMinutes + " 分钟内完成支付",
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
                // 超时关单记 EXPIRE，与用户主动取消的 CANCEL 区分开，时间轴才说得清是谁关的
                ((OrderService) org.springframework.aop.framework.AopContext.currentProxy())
                        .cancel(order.getOrderNo(), order.getUserId(), OrderAction.EXPIRE,
                                "超过 " + payTimeoutMinutes + " 分钟未支付，系统自动关闭，座位已释放，库存已归还");
                count++;
            } catch (Exception e) {
                log.warn("超时关单失败，仅置状态：orderNo={}, msg={}", order.getOrderNo(), e.getMessage());
                orderMapper.updateStatus(order.getOrderNo(), Constants.ORDER_STATUS_EXPIRED,
                        Constants.ORDER_STATUS_WAIT_PAY);
                paymentService.closeByOrderNo(order.getOrderNo(), "订单已超时");
                orderLogService.log(order.getOrderNo(), OrderAction.EXPIRE,
                        "超时关单降级处理（关单异常：" + e.getMessage() + "），仅置为已超时", "system");
            }
        }
        if (count > 0) {
            log.info("超时未支付订单自动关闭：{} 笔", count);
        }
        return count;
    }

    // ==================== private ====================

    /** 抢票任务的乘车区间：任务带了站序就用它，否则按车次时刻表解析（无时刻表即全程票） */
    private RangeBO rangeOf(SeckillTaskBO taskBO) {
        if (taskBO.getFromStopOrder() != null && taskBO.getToStopOrder() != null
                && taskBO.getToStopOrder() > taskBO.getFromStopOrder()) {
            return RangeBO.builder()
                    .fromOrder(taskBO.getFromStopOrder())
                    .toOrder(taskBO.getToStopOrder())
                    .build();
        }
        return segmentStockService.resolveRange(taskBO.getTrainId(), null, null);
    }

    /** 订单的乘车区间：订单落库时已快照站序，缺失时按车次时刻表补解析 */
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

    /** 释放 1 张购票额度（限购标记现在是计数器，减到 0 才删 key） */
    private void releaseQuota(Long trainId, Long userId) {
        try {
            String key = RedisKeys.seckillUser(trainId, userId);
            Long left = stringRedisTemplate.opsForValue().decrement(key, 1);
            if (left == null || left <= 0) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("释放购票额度失败（不影响主流程）：trainId={}, userId={}, msg={}", trainId, userId, e.getMessage());
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
                .refundFee(order.getRefundFee())
                .refundAmount(order.getRefundAmount())
                .changed(order.getChanged())
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



}

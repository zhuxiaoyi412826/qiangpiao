package com.qiangpiao.service.impl;

import com.qiangpiao.bo.OrderBO;
import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.OrderNoGenerator;
import com.qiangpiao.dataobject.SeckillRecordDO;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.dto.SeckillDTO;
import com.qiangpiao.mapper.SeckillRecordMapper;
import com.qiangpiao.mapper.TrainMapper;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.OrderService;
import com.qiangpiao.service.PurchaseLimitService;
import com.qiangpiao.service.SeckillService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.SeckillResultVO;
import com.qiangpiao.vo.SeckillStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀抢票核心实现：
 * <pre>
 *   1) 接口限流（Redis 计数器）
 *   2) 一人一单（Redis SETNX + DB 唯一索引）
 *   3) 车次状态校验（三级缓存）
 *   4) Redis Lua 原子预扣库存（防超卖）
 *   5) 异步线程池落库（削峰），失败补偿回滚
 *   6) 返回排队中，前端轮询结果
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {

    /** 脚本返回：库存未初始化 */
    private static final long STOCK_NOT_INIT = -1L;
    /** 脚本返回：库存不足 */
    private static final long STOCK_EMPTY = -2L;

    /** 一人一单标记有效时长（分钟） */
    private static final long USER_MARK_MINUTES = 30;
    /** 抢票结果缓存时长（分钟） */
    private static final long RESULT_MINUTES = 30;
    /** 抢票结果：异步落库失败前缀（让前端能拿到具体失败原因，而不是笼统的 -1） */
    private static final String FAIL_PREFIX = "FAIL:";

    private final StringRedisTemplate stringRedisTemplate;
    @Qualifier("seckillStockScript")
    private final RedisScript<Long> seckillStockScript;
    @Qualifier("rollbackStockScript")
    private final RedisScript<Long> rollbackStockScript;
    private final TrainService trainService;
    private final OrderService orderService;
    private final TrainStockMapper trainStockMapper;
    private final TrainMapper trainMapper;
    private final SeckillRecordMapper seckillRecordMapper;
    private final PurchaseLimitService purchaseLimitService;

    @Qualifier("seckillExecutor")
    private final Executor seckillExecutor;

    @Value("${seckill.user-limit}")
    private int userLimit;

    @Override
    public SeckillResultVO seckill(Long userId, SeckillDTO seckillDTO, String ip) {
        Long trainId = seckillDTO.getTrainId();
        Integer seatType = seckillDTO.getSeatType();

        // ---------- 1. 限流：单用户每分钟最多 userLimit 次 ----------
        String limitKey = RedisKeys.userLimit(userId);
        Long count = stringRedisTemplate.opsForValue().increment(limitKey, 1);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        }
        if (count != null && count > userLimit) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS);
        }

        // ---------- 2. 限购标记：每人每天每车次 1 张（不分席别，Redis SETNX 快速拦截） ----------
        String userKey = RedisKeys.seckillUser(trainId, userId);
        Boolean absent = stringRedisTemplate.opsForValue()
                .setIfAbsent(userKey, "1", USER_MARK_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(absent)) {
            throw new BizException(ResultCode.BUY_LIMIT_PER_TRAIN);
        }

        // ---------- 2.1 一人一单 DB 兜底 ----------
        // Redis 标记只有 30min，过期后同步层就失去了限购能力，只能等异步落库撞唯一索引，
        // 而异步线程的异常无法抛到 MVC 异常处理器，前端会误以为抢票成功。这里以 DB 秒杀记录为准再判一次。
        SeckillRecordDO bought = seckillRecordMapper.selectByUserAndTrain(trainId, seatType, userId);
        if (bought != null) {
            stringRedisTemplate.delete(userKey);
            log.info("重复抢票被同步拦截（DB 记录命中）：userId={}, trainId={}, seatType={}, recordOrderNo={}",
                    userId, trainId, seatType, bought.getOrderNo());
            throw new BizException(ResultCode.SECKILL_REPEAT);
        }

        // ---------- 3. 车次校验（含售票规则：未发车 / 预售期 / 开车前停售） ----------
        TrainBO trainBO = trainService.getTrainBO(trainId);
        if (!trainBO.onSale()) {
            stringRedisTemplate.delete(userKey);
            throw new BizException(ResultCode.TRAIN_NOT_SALE);
        }
        try {
            trainService.assertTicketSellable(trainId);
        } catch (RuntimeException e) {
            // 不可购票时释放限购标记，避免用户换个时间点重试被误判为重复抢票
            stringRedisTemplate.delete(userKey);
            throw e;
        }

        // ---------- 3.1 限购规则 ----------
        // 规则一：每人每天每车次限购 1 张（不分席别，DB 为准）
        // 规则二：已购车次处于运行时间内（发车~到达）不能重复购票，必须等下车（到达）后才行
        try {
            purchaseLimitService.assertCanBuy(userId, trainBO);
        } catch (RuntimeException e) {
            stringRedisTemplate.delete(userKey);
            throw e;
        }

        // ---------- 4. Redis 原子预扣库存 ----------
        String stockKey = RedisKeys.seckillStock(trainId, seatType);
        Long left = stringRedisTemplate.execute(seckillStockScript, Collections.singletonList(stockKey));
        if (left == null || left == STOCK_NOT_INIT) {
            // 未预热，先预热再重试一次
            preheatStock(trainId);
            left = stringRedisTemplate.execute(seckillStockScript, Collections.singletonList(stockKey));
        }
        if (left == null || left == STOCK_NOT_INIT || left == STOCK_EMPTY) {
            stringRedisTemplate.delete(userKey);
            throw new BizException(left == null || left == STOCK_NOT_INIT
                    ? ResultCode.STOCK_NOT_INIT : ResultCode.STOCK_NOT_ENOUGH);
        }

        // ---------- 5. 生成订单号，投递异步任务 ----------
        String orderNo = OrderNoGenerator.generate(userId);
        stringRedisTemplate.opsForValue()
                .set(RedisKeys.seckillResult(trainId, seatType, userId), orderNo, RESULT_MINUTES, TimeUnit.MINUTES);

        SeckillTaskBO taskBO = SeckillTaskBO.builder()
                .orderNo(orderNo)
                .userId(userId)
                .trainId(trainId)
                .seatType(seatType)
                .seatId(seckillDTO.getSeatId())
                .passengerName(seckillDTO.getPassengerName())
                .idCard(seckillDTO.getIdCard())
                .clientIp(ip)
                .build();
        seckillExecutor.execute(() -> asyncCreateOrder(taskBO));

        log.info("秒杀库存预扣成功（进入异步队列）：userId={}, trainId={}, seatType={}, orderNo={}, 剩余={}",
                userId, trainId, seatType, orderNo, left);
        return SeckillResultVO.builder()
                .orderNo(orderNo)
                .status(SeckillResultVO.STATUS_QUEUEING)
                .message("抢票请求已受理，正在为您锁定座位…")
                .trainId(trainId)
                .seatType(seatType)
                .build();
    }

    @Override
    public SeckillStatusVO queryResult(Long trainId, Integer seatType, Long userId) {
        String resultValue = stringRedisTemplate.opsForValue()
                .get(RedisKeys.seckillResult(trainId, seatType, userId));

        // 异步落库失败时，补偿流程会把失败原因写在这里，优先返回给前端
        if (resultValue != null && resultValue.startsWith(FAIL_PREFIX)) {
            return SeckillStatusVO.builder()
                    .status(-1)
                    .message(resultValue.substring(FAIL_PREFIX.length()))
                    .build();
        }

        String orderNo = resultValue;
        if (orderNo == null) {
            SeckillRecordDO record = seckillRecordMapper.selectByUserAndTrain(trainId, seatType, userId);
            if (record != null) {
                orderNo = record.getOrderNo();
            }
        }
        if (orderNo == null) {
            return SeckillStatusVO.builder()
                    .status(-1)
                    .message("未查询到抢票记录，可能已售罄或请求已过期")
                    .build();
        }
        OrderBO orderBO = null;
        try {
            orderBO = orderService.getOrderBO(orderNo);
        } catch (BizException e) {
            // 订单还未落库（异步仍在排队）或已被补偿清理，按排队中处理
            log.debug("订单尚未落库，继续排队：orderNo={}, msg={}", orderNo, e.getMessage());
        }
        if (orderBO == null || orderBO.getId() == null) {
            return SeckillStatusVO.builder().orderNo(orderNo).status(0).message("排队中，请稍后…").build();
        }
        return SeckillStatusVO.builder()
                .orderNo(orderNo)
                .status(1)
                .carriageNo(orderBO.getCarriageNo())
                .seatNo(orderBO.getSeatNo())
                .message("抢票成功")
                .build();
    }

    @Override
    public void preheatStock(Long trainId) {
        List<TrainStockDO> stocks = trainStockMapper.selectByTrainId(trainId);
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        for (TrainStockDO stock : stocks) {
            stringRedisTemplate.opsForValue()
                    .set(RedisKeys.seckillStock(trainId, stock.getSeatType()),
                            String.valueOf(stock.getAvailableCount() == null ? 0 : stock.getAvailableCount()),
                            12, TimeUnit.HOURS);
        }
        log.info("预热秒杀库存完成：trainId={}, 席别数={}", trainId, stocks.size());
    }

    @Override
    public void preheatAllStock() {
        long total = trainMapper.countAll();
        int pageSize = 100;
        for (long offset = 0; offset < total; offset += pageSize) {
            List<TrainDO> trains = trainMapper.selectAll(offset, (long) pageSize);
            for (TrainDO train : trains) {
                preheatStock(train.getId());
            }
        }
        log.info("全量预热秒杀库存完成，车次数={}", total);
    }

    @Override
    public int availableStock(Long trainId, Integer seatType) {
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.seckillStock(trainId, seatType));
        if (value == null) {
            return -1;
        }
        try {
            return Math.max(Integer.parseInt(value), 0);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public void rollbackStock(Long trainId, Integer seatType) {
        String stockKey = RedisKeys.seckillStock(trainId, seatType);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            stringRedisTemplate.execute(rollbackStockScript, Collections.singletonList(stockKey));
        }
    }

    // ==================== private ====================

    /**
     * 异步落库：失败则补偿（回滚 Redis 库存 + 清理标记）
     */
    private void asyncCreateOrder(SeckillTaskBO taskBO) {
        long start = System.currentTimeMillis();
        try {
            orderService.createSeckillOrder(taskBO);
        } catch (Exception e) {
            String reason = ResultCode.SECKILL_FAILED.getMessage();
            if (e instanceof BizException && e.getMessage() != null && !e.getMessage().isEmpty()) {
                reason = e.getMessage();
            }
            log.error("异步创建秒杀订单失败，执行补偿：orderNo={}, msg={}",
                    taskBO.getOrderNo(), e.getMessage());
            compensate(taskBO, reason);
        } finally {
            log.info("异步下单耗时：{} ms, orderNo={}", System.currentTimeMillis() - start, taskBO.getOrderNo());
        }
    }

    private void compensate(SeckillTaskBO taskBO, String reason) {
        try {
            rollbackStock(taskBO.getTrainId(), taskBO.getSeatType());
            stringRedisTemplate.delete(RedisKeys.seckillUser(taskBO.getTrainId(), taskBO.getUserId()));
            // 把失败原因写回结果 key（而不是直接删除），让前端轮询时能拿到明确提示
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.seckillResult(taskBO.getTrainId(), taskBO.getSeatType(), taskBO.getUserId()),
                    FAIL_PREFIX + reason, RESULT_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("秒杀补偿失败（需人工介入）：orderNo={}", taskBO.getOrderNo(), e);
        }
    }
}

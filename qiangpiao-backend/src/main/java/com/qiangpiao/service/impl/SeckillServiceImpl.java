package com.qiangpiao.service.impl;

import com.qiangpiao.bo.OrderBO;
import com.qiangpiao.bo.RangeBO;
import com.qiangpiao.bo.RiskCheckBO;
import com.qiangpiao.bo.SeckillTaskBO;
import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.OrderAction;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.SensitiveCrypto;
import com.qiangpiao.common.util.OrderNoGenerator;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.dataobject.SeckillFlowDO;
import com.qiangpiao.dataobject.SeckillRecordDO;
import com.qiangpiao.dataobject.TrainDO;
import com.qiangpiao.dataobject.TrainStockDO;
import com.qiangpiao.dto.PassengerItemDTO;
import com.qiangpiao.dto.SeckillDTO;
import com.qiangpiao.mapper.SeckillRecordMapper;
import com.qiangpiao.mapper.TrainMapper;
import com.qiangpiao.mapper.TrainStockMapper;
import com.qiangpiao.service.CaptchaService;
import com.qiangpiao.service.OrderLogService;
import com.qiangpiao.service.OrderService;
import com.qiangpiao.service.PassengerService;
import com.qiangpiao.service.PurchaseLimitService;
import com.qiangpiao.service.RateLimitService;
import com.qiangpiao.service.RiskControlService;
import com.qiangpiao.service.SeckillFlowService;
import com.qiangpiao.service.SeckillService;
import com.qiangpiao.service.SeckillSseService;
import com.qiangpiao.service.SegmentStockService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.SeckillBatchResultVO;
import com.qiangpiao.vo.SeckillFlowVO;
import com.qiangpiao.vo.SeckillPushVO;
import com.qiangpiao.vo.SeckillResultVO;
import com.qiangpiao.vo.SeckillStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    private final PassengerService passengerService;
    private final SensitiveCrypto crypto;
    private final OrderLogService orderLogService;
    private final SeckillFlowService seckillFlowService;
    private final RateLimitService rateLimitService;
    private final RiskControlService riskControlService;
    private final CaptchaService captchaService;
    private final SegmentStockService segmentStockService;
    private final SeckillSseService seckillSseService;

    @Qualifier("seckillExecutor")
    private final Executor seckillExecutor;

    /** 抢票是否强制人机验证（true = 每次抢票都要先过验证码） */
    @Value("${seckill.captcha-enabled:true}")
    private boolean captchaEnabled;

    @Override
    public SeckillResultVO seckill(Long userId, SeckillDTO seckillDTO, String ip) {
        Long trainId = seckillDTO.getTrainId();
        Integer seatType = seckillDTO.getSeatType();

        // ---------- 0. 解析乘客：一次可买多张，每位乘客一张票（最多 9 张） ----------
        List<PlainTicket> tickets = resolveTickets(userId, seckillDTO);
        int count = tickets.size();
        List<Long> seatIds = seckillDTO.getSeatIds() == null ? Collections.emptyList() : seckillDTO.getSeatIds();
        if (!seatIds.isEmpty() && seatIds.size() != count) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }

        // ---------- 1. 限流：全局令牌桶 + IP 滑动窗口 + 用户滑动窗口 ----------
        rateLimitService.assertSeckillAllowed(userId, ip);

        // ---------- 1.1 风控：同一 IP 多账号 / 极短耗时请求 ----------
        RiskCheckBO risk = riskControlService.check(userId, ip);
        if (risk.isHit()) {
            log.warn("抢票风控命中：userId={}, ip={}, reasons={}, 累计={} 次",
                    userId, ip, risk.getReasons(), risk.getHits());
        }

        // ---------- 1.2 人机验证：抢票前置，图形验证码或滑块二选一 ----------
        if (captchaEnabled || risk.isHit()) {
            assertCaptcha(seckillDTO);
        }

        // ---------- 2. 车次校验（含售票规则：未发车 / 预售期 / 开车前停售） ----------
        TrainBO trainBO = trainService.getTrainBO(trainId);
        if (!trainBO.onSale()) {
            throw new BizException(ResultCode.TRAIN_NOT_SALE);
        }
        trainService.assertTicketSellable(trainId);

        // ---------- 3. 限购：同一车次最多 9 张 + 同一乘车人不可重复（DB 为准） ----------
        List<String> ciphers = new ArrayList<>(count);
        for (PlainTicket ticket : tickets) {
            ciphers.add(crypto.encrypt(ticket.getIdCard()));
        }
        try {
            purchaseLimitService.assertCanBuyBatch(userId, trainBO, count, ciphers);
        } catch (RuntimeException e) {
            log.info("批量购票被限购拦截：userId={}, trainId={}, 张数={}, msg={}",
                    userId, trainId, count, e.getMessage());
            throw e;
        }

        // ---------- 4. 抢购额度标记（Redis 计数，按本次张数累加） ----------
        String userKey = RedisKeys.seckillUser(trainId, userId);
        Long quota = stringRedisTemplate.opsForValue().increment(userKey, count);
        if (quota != null && quota == (long) count) {
            stringRedisTemplate.expire(userKey, USER_MARK_MINUTES, TimeUnit.MINUTES);
        }
        if (quota != null && quota > PurchaseLimitService.MAX_TICKETS_PER_TRAIN) {
            releaseQuota(trainId, userId, count);
            throw new BizException(ResultCode.BUY_LIMIT_PER_TRAIN);
        }

        // ---------- 4.1 DB 兜底：同一乘车人 + 同一车次 + 同一席别已抢过 ----------
        for (String cipher : ciphers) {
            SeckillRecordDO bought =
                    seckillRecordMapper.selectByUserTrainAndIdCard(trainId, seatType, userId, cipher);
            if (bought != null) {
                releaseQuota(trainId, userId, count);
                log.info("重复抢票被同步拦截（DB 记录命中）：userId={}, trainId={}, recordOrderNo={}",
                        userId, trainId, bought.getOrderNo());
                throw new BizException(ResultCode.PASSENGER_TICKET_EXISTS);
            }
        }

        // ---------- 5. Redis 原子预扣库存 ----------
        //     区间票：一次抢票要占用「上车站 → 下车站」覆盖的每一段，一段不足则整批失败（不部分成交）
        //     全程票：沿用原来的单 key 整批扣减
        RangeBO range = segmentStockService.resolveRange(trainId,
                seckillDTO.getFromStation(), seckillDTO.getToStation());
        boolean segmentMode = segmentStockService.enabled()
                && segmentStockService.available(trainId, seatType, range) != null;
        long left = 0;
        if (segmentMode) {
            int segResult = segmentStockService.deductRedis(trainId, seatType, range, count);
            if (segResult == -1) {
                // 段库存未预热，初始化后再试一次
                segmentStockService.initSegments(trainId);
                segResult = segmentStockService.deductRedis(trainId, seatType, range, count);
            }
            if (segResult != 1) {
                releaseQuota(trainId, userId, count);
                throw new BizException(segResult == -1
                        ? ResultCode.STOCK_NOT_INIT : ResultCode.STOCK_NOT_ENOUGH);
            }
        } else {
            String stockKey = RedisKeys.seckillStock(trainId, seatType);
            String countArg = String.valueOf(count);
            Long stockLeft = stringRedisTemplate.execute(seckillStockScript,
                    Collections.singletonList(stockKey), countArg);
            if (stockLeft == null || stockLeft == STOCK_NOT_INIT) {
                // 未预热，先预热再重试一次
                preheatStock(trainId);
                stockLeft = stringRedisTemplate.execute(seckillStockScript,
                        Collections.singletonList(stockKey), countArg);
            }
            if (stockLeft == null || stockLeft == STOCK_NOT_INIT || stockLeft == STOCK_EMPTY) {
                releaseQuota(trainId, userId, count);
                throw new BizException(stockLeft == null || stockLeft == STOCK_NOT_INIT
                        ? ResultCode.STOCK_NOT_INIT : ResultCode.STOCK_NOT_ENOUGH);
            }
            left = stockLeft;
        }

        // ---------- 6. 生成批次号 + N 个订单号，逐张投递异步任务 ----------
        //     排队进度：该「车次 + 席别」维度维护「累计受理序号」和「待处理票数」，
        //     受理时 +N，落库成功 / 失败补偿时 -1，前端据此提示「你是第几位 / 前面还有几张」。
        Long queueSeq = incrQueue(RedisKeys.seckillQueueSeq(trainId, seatType), 1);
        Long queueAhead = incrQueue(RedisKeys.seckillQueuePending(trainId, seatType), count);

        String batchNo = generateBatchNo();
        List<String> orderNos = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            PlainTicket ticket = tickets.get(i);
            String orderNo = OrderNoGenerator.generate(userId);
            orderNos.add(orderNo);
            // 批次内单张票的状态：状态|乘客姓名|车厢|座位|信息
            stringRedisTemplate.opsForValue().set(RedisKeys.seckillTicket(orderNo),
                    "0|" + ticket.getPassengerName() + "|||", RESULT_MINUTES, TimeUnit.MINUTES);
            // 抢票受理留痕：从这一步开始订单号就有了业务日志，后续成功 / 失败都能串起来
            orderLogService.logAsync(orderNo, OrderAction.SECKILL_ACCEPT,
                    "抢票请求已受理，批次 " + batchNo + " 第 " + (i + 1) + "/" + count + " 张，排队序号 " + queueSeq,
                    String.valueOf(userId));
            SeckillTaskBO taskBO = SeckillTaskBO.builder()
                    .orderNo(orderNo)
                    .userId(userId)
                    .trainId(trainId)
                    .seatType(seatType)
                    .seatId(i < seatIds.size() ? seatIds.get(i) : null)
                    .batchNo(batchNo)
                    .ticketIndex(i + 1)
                    .queueSeq(queueSeq)
                    .passengerName(ticket.getPassengerName())
                    .idCard(ticket.getIdCard())
                    .clientIp(ip)
                    .fromStopOrder(range.getFromOrder())
                    .toStopOrder(range.getToOrder())
                    .build();
            // 抢票流水：受理即留痕（异步写，后面出结果再回写状态与耗时）
            SeckillFlowDO flow = new SeckillFlowDO();
            flow.setBatchNo(batchNo);
            flow.setOrderNo(orderNo);
            flow.setUserId(userId);
            flow.setTrainId(trainId);
            flow.setSeatType(seatType);
            flow.setPassengerName(ticket.getPassengerName());
            flow.setTicketIndex(i + 1);
            flow.setQueueSeq(queueSeq);
            flow.setClientIp(ip);
            flow.setStatus(SeckillFlowVO.STATUS_QUEUEING);
            seckillFlowService.accept(flow);
            seckillExecutor.execute(() -> asyncCreateOrder(taskBO));
        }
        stringRedisTemplate.opsForValue().set(RedisKeys.seckillBatch(batchNo), String.join(",", orderNos),
                RESULT_MINUTES, TimeUnit.MINUTES);
        // 兼容旧的单张轮询（取批次内第一张）
        stringRedisTemplate.opsForValue().set(RedisKeys.seckillResult(trainId, seatType, userId),
                orderNos.get(0), RESULT_MINUTES, TimeUnit.MINUTES);

        log.info("秒杀库存预扣成功（进入异步队列）：userId={}, trainId={}, seatType={}, batchNo={}, 张数={}, 剩余={}",
                userId, trainId, seatType, batchNo, count, left);
        return SeckillResultVO.builder()
                .orderNo(orderNos.get(0))
                .batchNo(batchNo)
                .count(count)
                .orderNos(orderNos)
                .status(SeckillResultVO.STATUS_QUEUEING)
                .message(count > 1
                        ? "共 " + count + " 张票，抢票请求已受理，正在为您锁定座位…"
                        : "抢票请求已受理，正在为您锁定座位…")
                .trainId(trainId)
                .seatType(seatType)
                .queueSeq(queueSeq)
                .queueAhead(Math.max(queueAhead - count, 0))
                .build();
    }

    @Override
    public SeckillBatchResultVO queryBatchResult(Long userId, String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        String joined = stringRedisTemplate.opsForValue().get(RedisKeys.seckillBatch(batchNo));
        if (joined == null) {
            return SeckillBatchResultVO.builder()
                    .batchNo(batchNo)
                    .count(0)
                    .status(SeckillBatchResultVO.STATUS_FAILED)
                    .message("抢票批次不存在或已过期，请重新下单")
                    .items(Collections.emptyList())
                    .build();
        }
        List<String> orderNos = Arrays.stream(joined.split(","))
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());

        List<SeckillBatchResultVO.TicketResult> items = new ArrayList<>(orderNos.size());
        int success = 0;
        int failed = 0;
        int pending = 0;
        for (String orderNo : orderNos) {
            String value = stringRedisTemplate.opsForValue().get(RedisKeys.seckillTicket(orderNo));
            SeckillBatchResultVO.TicketResult item = parseTicketResult(orderNo, value);
            items.add(item);
            if (item.getStatus() == null || item.getStatus() == 0) {
                pending++;
            } else if (item.getStatus() == 1) {
                success++;
            } else {
                failed++;
            }
        }
        int status;
        String message;
        if (pending > 0) {
            status = SeckillBatchResultVO.STATUS_QUEUEING;
            message = "正在为您锁定座位（" + success + "/" + orderNos.size() + "）…";
        } else if (failed == 0) {
            status = SeckillBatchResultVO.STATUS_SUCCESS;
            message = orderNos.size() > 1 ? "全部抢票成功，共 " + success + " 张" : "抢票成功";
        } else if (success == 0) {
            status = SeckillBatchResultVO.STATUS_FAILED;
            message = "抢票失败，请重试";
        } else {
            status = SeckillBatchResultVO.STATUS_PARTIAL;
            message = "部分成功：成功 " + success + " 张，失败 " + failed + " 张（失败的票已释放）";
        }
        return SeckillBatchResultVO.builder()
                .batchNo(batchNo)
                .count(orderNos.size())
                .status(status)
                .message(message)
                .items(items)
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
        // 区间票：顺便初始化「相邻站单段」库存并预热到 Redis（没建表时内部直接返回 0）
        segmentStockService.initSegments(trainId);
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
        rollbackStock(trainId, seatType, 1);
    }

    /**
     * 人机验证：图形验证码或滑块二选一。
     * 未携带任何验证信息 → 提示先验证；携带但校验失败 → 验证无效。
     */
    private void assertCaptcha(SeckillDTO seckillDTO) {
        if (StringUtils.hasText(seckillDTO.getCaptchaId())) {
            if (!captchaService.verifyImage(seckillDTO.getCaptchaId(), seckillDTO.getCaptchaCode())) {
                throw new BizException(ResultCode.CAPTCHA_INVALID);
            }
            return;
        }
        if (StringUtils.hasText(seckillDTO.getSliderId())) {
            if (!captchaService.verifySlider(seckillDTO.getSliderId(), seckillDTO.getSliderX())) {
                throw new BizException(ResultCode.CAPTCHA_INVALID);
            }
            return;
        }
        throw new BizException(ResultCode.CAPTCHA_REQUIRED);
    }

    /** 回滚 Redis 库存（count = 张数，批量下单失败时整批回滚） */
    private void rollbackStock(Long trainId, Integer seatType, int count) {
        rollbackStock(trainId, seatType, count, null);
    }

    /**
     * 回滚 Redis 库存（区间票版）：区间模式下归还「该区间覆盖的每一段」，
     * 否则退回单 key 回滚。
     */
    private void rollbackStock(Long trainId, Integer seatType, int count, RangeBO range) {
        RangeBO safeRange = range != null ? range : segmentStockService.resolveRange(trainId, null, null);
        if (segmentStockService.enabled()
                && !segmentStockService.stockKeys(trainId, seatType, safeRange).isEmpty()) {
            segmentStockService.rollbackRedis(trainId, seatType, safeRange, count);
            return;
        }
        String stockKey = RedisKeys.seckillStock(trainId, seatType);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(stockKey))) {
            stringRedisTemplate.execute(rollbackStockScript, Collections.singletonList(stockKey),
                    String.valueOf(count));
        }
    }

    /**
     * 推送一张票的抢票结果（SSE）：推送失败只记 debug 日志，前端会降级为轮询，不影响抢票。
     */
    private void pushResult(SeckillTaskBO taskBO, int status, Integer carriageNo, String seatNo, String message) {
        if (taskBO.getBatchNo() == null) {
            return;
        }
        try {
            // 批次是否已全部出结果：出结果后前端即可停止轮询
            boolean finished = queryBatchResult(taskBO.getUserId(), taskBO.getBatchNo()).getStatus()
                    != SeckillBatchResultVO.STATUS_QUEUEING;
            seckillSseService.push(taskBO.getUserId(), taskBO.getBatchNo(), SeckillPushVO.builder()
                    .batchNo(taskBO.getBatchNo())
                    .orderNo(taskBO.getOrderNo())
                    .status(status)
                    .carriageNo(carriageNo)
                    .seatNo(seatNo)
                    .message(message)
                    .finished(finished)
                    .build());
        } catch (Exception e) {
            log.debug("推送抢票结果失败（前端将降级为轮询）：orderNo={}, msg={}",
                    taskBO.getOrderNo(), e.getMessage());
        }
    }

    /** 任务的乘车区间：任务带了站序就用它，否则按车次时刻表解析（无时刻表即全程票） */
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

    // ==================== private ====================

    /**
     * 异步落库：失败则补偿（回滚 Redis 库存 + 清理标记）
     */
    private void asyncCreateOrder(SeckillTaskBO taskBO) {
        long start = System.currentTimeMillis();
        try {
            // 同批次优先坐一起：本批次已有票落位时，跟着坐那个车厢（坐不下会自动改分其它车厢）
            if (taskBO.getBatchNo() != null && taskBO.getPreferCarriageNo() == null) {
                String carriage = stringRedisTemplate.opsForValue()
                        .get(RedisKeys.seckillBatchCarriage(taskBO.getBatchNo()));
                if (StringUtils.hasText(carriage)) {
                    try {
                        taskBO.setPreferCarriageNo(Integer.valueOf(carriage.trim()));
                    } catch (NumberFormatException ignored) {
                        // 脏数据忽略，退化成自动分配
                    }
                }
            }
            OrderDO order = orderService.createSeckillOrder(taskBO);
            // 第一张落位的车厢写入批次，供同批次后续票参考
            if (taskBO.getBatchNo() != null && order.getCarriageNo() != null) {
                String carriageKey = RedisKeys.seckillBatchCarriage(taskBO.getBatchNo());
                if (Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                        .setIfAbsent(carriageKey, String.valueOf(order.getCarriageNo())))) {
                    stringRedisTemplate.expire(carriageKey, RESULT_MINUTES, TimeUnit.MINUTES);
                }
            }
            // 成功：写入批次内该票的结果（车厢 + 座位）
            stringRedisTemplate.opsForValue().set(RedisKeys.seckillTicket(taskBO.getOrderNo()),
                    "1|" + order.getPassengerName() + "|" + order.getCarriageNo() + "|" + order.getSeatNo() + "|",
                    RESULT_MINUTES, TimeUnit.MINUTES);
            seckillFlowService.finish(taskBO.getOrderNo(), SeckillFlowVO.STATUS_SUCCESS, null,
                    System.currentTimeMillis() - start);
            // SSE 主动推送，前端不必等下一轮轮询
            pushResult(taskBO, 1, order.getCarriageNo(), order.getSeatNo(), "抢票成功");
        } catch (Exception e) {
            String reason = ResultCode.SECKILL_FAILED.getMessage();
            if (e instanceof BizException && e.getMessage() != null && !e.getMessage().isEmpty()) {
                reason = e.getMessage();
            }
            log.error("异步创建秒杀订单失败，执行补偿：orderNo={}, msg={}",
                    taskBO.getOrderNo(), e.getMessage());
            // 抢票失败也要留痕：订单没落库时，这条日志是用户能查到的唯一凭证
            orderLogService.logAsync(taskBO.getOrderNo(), OrderAction.SECKILL_FAIL,
                    "抢票失败：" + reason + "，Redis 库存与购票额度已回滚，可重新抢票",
                    String.valueOf(taskBO.getUserId()));
            seckillFlowService.finish(taskBO.getOrderNo(), SeckillFlowVO.STATUS_FAILED, reason,
                    System.currentTimeMillis() - start);
            compensate(taskBO, reason);
            pushResult(taskBO, -1, null, null, reason);
        } finally {
            // 无论成功失败，该票都已出队，排队计数回退
            incrQueue(RedisKeys.seckillQueuePending(taskBO.getTrainId(), taskBO.getSeatType()), -1);
            log.info("异步下单耗时：{} ms, orderNo={}", System.currentTimeMillis() - start, taskBO.getOrderNo());
        }
    }

    /** 排队计数自增（可为负）：Redis 异常不影响抢票主流程 */
    private Long incrQueue(String key, long delta) {
        try {
            Long value = stringRedisTemplate.opsForValue().increment(key, delta);
            return value == null ? 0L : value;
        } catch (Exception e) {
            log.warn("更新排队计数失败（不影响抢票）：key={}, msg={}", key, e.getMessage());
            return 0L;
        }
    }

    private void compensate(SeckillTaskBO taskBO, String reason) {
        try {
            rollbackStock(taskBO.getTrainId(), taskBO.getSeatType(), 1, rangeOf(taskBO));
            releaseQuota(taskBO.getTrainId(), taskBO.getUserId(), 1);
            // 批次内该票标记失败（只影响这一张，同批次其它票不受影响）
            stringRedisTemplate.opsForValue().set(RedisKeys.seckillTicket(taskBO.getOrderNo()),
                    "-1|" + taskBO.getPassengerName() + "|||" + reason, RESULT_MINUTES, TimeUnit.MINUTES);
            // 兼容旧的单张轮询：把失败原因写回结果 key，让前端能拿到明确提示
            stringRedisTemplate.opsForValue().set(
                    RedisKeys.seckillResult(taskBO.getTrainId(), taskBO.getSeatType(), taskBO.getUserId()),
                    FAIL_PREFIX + reason, RESULT_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("秒杀补偿失败（需人工介入）：orderNo={}", taskBO.getOrderNo(), e);
        }
    }

    // ==================== 批量购票辅助 ====================

    /**
     * 解析本次购票的乘客列表：支持一次买多张（每位乘客一张票），最多 9 张。
     * 传 passengerId 时由服务端解密取明文，前端不必传证件号。
     */
    private List<PlainTicket> resolveTickets(Long userId, SeckillDTO dto) {
        List<PlainTicket> tickets = new ArrayList<>();
        List<PassengerItemDTO> items = dto.getPassengers();
        if (items != null && !items.isEmpty()) {
            for (PassengerItemDTO item : items) {
                if (item == null) {
                    throw new BizException(ResultCode.BAD_REQUEST);
                }
                if (item.getPassengerId() != null) {
                    PassengerService.PlainPassenger p =
                            passengerService.resolve(userId, item.getPassengerId());
                    tickets.add(new PlainTicket(p.getPassengerName(), p.getIdCard(), null));
                } else {
                    tickets.add(new PlainTicket(checkName(item.getPassengerName()),
                            checkIdCard(item.getIdCard()), checkPhone(item.getPhone())));
                }
            }
        } else {
            // 兼容原来的单张下单入参
            String name = dto.getPassengerName();
            String idCard = dto.getIdCard();
            if (dto.getPassengerId() != null) {
                PassengerService.PlainPassenger p = passengerService.resolve(userId, dto.getPassengerId());
                name = p.getPassengerName();
                idCard = p.getIdCard();
            }
            tickets.add(new PlainTicket(checkName(name), checkIdCard(idCard), checkPhone(dto.getPhone())));
        }
        if (tickets.size() > PurchaseLimitService.MAX_TICKETS_PER_TRAIN) {
            throw new BizException(ResultCode.SECKILL_BATCH_LIMIT);
        }
        return tickets;
    }

    private String checkName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        return name.trim();
    }

    private String checkIdCard(String idCard) {
        if (!StringUtils.hasText(idCard) || !crypto.validIdCard(idCard.trim())) {
            throw new BizException(ResultCode.ID_CARD_INVALID);
        }
        return idCard.trim();
    }

    /** 临时乘客手机号：选填，填了必须合规（位数 + 号段，含开发白名单） */
    private String checkPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        if (!crypto.validPhone(phone.trim())) {
            throw new BizException(ResultCode.PHONE_INVALID);
        }
        return phone.trim();
    }

    /** 释放抢购额度（成批扣减，减到 0 直接删 key） */
    private void releaseQuota(Long trainId, Long userId, int count) {
        try {
            String key = RedisKeys.seckillUser(trainId, userId);
            Long left = stringRedisTemplate.opsForValue().decrement(key, count);
            if (left == null || left <= 0) {
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("释放抢购额度失败（不影响主流程）：trainId={}, userId={}, msg={}", trainId, userId, e.getMessage());
        }
    }

    private String generateBatchNo() {
        return "B" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    /** 解析批次内单张票的状态串：状态|乘客姓名|车厢|座位|信息 */
    private SeckillBatchResultVO.TicketResult parseTicketResult(String orderNo, String value) {
        if (value == null) {
            return SeckillBatchResultVO.TicketResult.builder()
                    .orderNo(orderNo).status(-1).message("抢票记录已过期").build();
        }
        String[] parts = value.split("\\|", -1);
        Integer status = 0;
        try {
            status = Integer.valueOf(parts[0]);
        } catch (Exception ignored) {
            status = -1;
        }
        Integer carriageNo = null;
        if (parts.length > 2 && StringUtils.hasText(parts[2])) {
            try {
                carriageNo = Integer.valueOf(parts[2]);
            } catch (Exception ignored) {
                carriageNo = null;
            }
        }
        return SeckillBatchResultVO.TicketResult.builder()
                .orderNo(orderNo)
                .passengerName(parts.length > 1 ? parts[1] : null)
                .status(status)
                .carriageNo(carriageNo)
                .seatNo(parts.length > 3 ? parts[3] : null)
                .message(parts.length > 4 ? parts[4] : null)
                .build();
    }

    /** 本次购票的单个乘客（明文，仅内存流转） */
    private static class PlainTicket {
        private final String passengerName;
        private final String idCard;
        /** 临时乘客手机号（选填，仅校验，当前订单模型不落库） */
        private final String phone;

        PlainTicket(String passengerName, String idCard, String phone) {
            this.passengerName = passengerName;
            this.idCard = idCard;
            this.phone = phone;
        }

        String getPassengerName() {
            return passengerName;
        }

        String getIdCard() {
            return idCard;
        }

        String getPhone() {
            return phone;
        }
    }
}

package com.qiangpiao.service.impl;

import com.qiangpiao.bo.TrainBO;
import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.constant.OrderAction;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.PaymentSignUtil;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dataobject.OrderDO;
import com.qiangpiao.dataobject.PaymentDO;
import com.qiangpiao.mapper.OrderMapper;
import com.qiangpiao.mapper.PaymentMapper;
import com.qiangpiao.service.OrderLogService;
import com.qiangpiao.service.PaymentService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.service.WalletService;
import com.qiangpiao.vo.PaymentVO;
import com.qiangpiao.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PreDestroy;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现：模拟第三方渠道的两阶段支付。
 * <pre>
 *   发起支付（不扣款，建支付单） -> 渠道异步回调（验签 + 幂等）-> 扣款 + 订单置为已支付
 * </pre>
 * 真接支付宝 / 微信时，只需把 {@link #scheduleMockCallback} 换成真实下单，
 * {@link #handleNotify(Map)} 保持原样即为渠道回调入口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** 回调成功标识 */
    private static final String RESULT_SUCCESS = "SUCCESS";
    /** 默认支付方式 */
    private static final String DEFAULT_PAY_TYPE = "ALIPAY";

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderLogService orderLogService;
    private final TrainService trainService;
    private final WalletService walletService;
    private final StringRedisTemplate stringRedisTemplate;
    /** 取自身代理调用事务方法：回调线程不在事务调用链里，AopContext 拿不到代理 */
    private final org.springframework.context.ApplicationContext applicationContext;

    /** 回调验签密钥 */
    @Value("${pay.secret}")
    private String paySecret;
    /** 回调时间戳有效窗口（毫秒） */
    @Value("${pay.notify-timestamp-window-ms:300000}")
    private long timestampWindowMs;
    /** nonce 防重放缓存时间（秒） */
    @Value("${pay.notify-nonce-ttl:600}")
    private long nonceTtlSeconds;
    /** 支付单有效期（分钟） */
    @Value("${pay.session-minutes:5}")
    private int sessionMinutes;
    /** 模拟渠道回调延迟（毫秒） */
    @Value("${pay.mock-callback-delay-ms:3000}")
    private long mockCallbackDelayMs;

    /** 模拟渠道的异步回调线程 */
    private final ScheduledExecutorService callbackScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "pay-mock-callback");
                t.setDaemon(true);
                return t;
            });

    @PreDestroy
    public void destroy() {
        callbackScheduler.shutdownNow();
    }

    // ==================== 第一阶段：发起支付 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentVO createPayment(String orderNo, Long userId, String payType, String idempotentKey) {
        TraceContext.putOrder(orderNo);
        TraceContext.putUser(userId);

        OrderDO order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (userId != null && !userId.equals(order.getUserId())) {
            throw new BizException(ResultCode.FORBIDDEN);
        }
        if (!Objects.equals(order.getStatus(), Constants.ORDER_STATUS_WAIT_PAY)) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR);
        }
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.ORDER_EXPIRED);
        }

        // 余额预校验：模拟渠道侧的余额/额度校验，避免回调时才失败导致订单悬挂
        BigDecimal amount = order.getPrice();
        WalletVO wallet = walletService.getWallet(order.getUserId());
        if (wallet.getBalance() == null || wallet.getBalance().compareTo(amount) < 0) {
            throw new BizException(ResultCode.WALLET_BALANCE_NOT_ENOUGH);
        }

        String type = (payType == null || payType.trim().isEmpty()) ? DEFAULT_PAY_TYPE : payType.trim().toUpperCase();
        String base = (idempotentKey == null || idempotentKey.trim().isEmpty())
                ? orderNo + ":" + type : idempotentKey.trim();

        // 幂等：同一幂等键存在「支付中」的流水时直接复用，重复点击不会生成第二笔扣款
        PaymentDO last = paymentMapper.selectLatestByIdempotent(base);
        if (last != null && Objects.equals(last.getStatus(), Constants.PAY_STATUS_PAYING)) {
            log.info("重复发起支付，复用原支付单：payNo={}, orderNo={}", last.getPayNo(), orderNo);
            return toVO(last);
        }

        PaymentDO payment = new PaymentDO();
        payment.setPayNo(generatePayNo(order.getUserId()));
        payment.setOrderNo(orderNo);
        payment.setUserId(order.getUserId());
        payment.setPayType(type);
        payment.setAmount(amount);
        payment.setStatus(Constants.PAY_STATUS_PAYING);
        // 重试场景（上一次失败/关闭）追加后缀，避免撞 uk_idempotent_key
        payment.setIdempotentKey(last == null ? base : base + "#" + last.getId());
        payment.setNotifyCount(0);
        LocalDateTime expire = LocalDateTime.now().plusMinutes(sessionMinutes);
        if (order.getExpireTime() != null && order.getExpireTime().isBefore(expire)) {
            expire = order.getExpireTime();
        }
        payment.setExpireTime(expire);
        paymentMapper.insert(payment);

        // 模拟渠道：事务提交后延迟触发异步回调（真实渠道由对方服务器回调 /notify）
        scheduleMockCallback(payment.getPayNo(), RESULT_SUCCESS);

        orderLogService.log(orderNo, OrderAction.PAY_CREATE,
                "支付单 " + payment.getPayNo() + " 已创建，金额 " + amount + " 元，方式 " + type + "，等待渠道回调",
                String.valueOf(order.getUserId()));
        log.info("发起支付成功：payNo={}, orderNo={}, userId={}, amount={}, payType={}",
                payment.getPayNo(), orderNo, order.getUserId(), amount, type);
        return toVO(payment);
    }

    @Override
    public PaymentVO getByPayNo(String payNo) {
        PaymentDO payment = paymentMapper.selectByPayNo(payNo);
        if (payment == null) {
            throw new BizException(ResultCode.PAYMENT_NOT_FOUND);
        }
        return toVO(payment);
    }

    // ==================== 第二阶段：渠道回调 ====================

    @Override
    public boolean handleNotify(Map<String, Object> params) {
        String payNo = str(params.get("payNo"));
        String tradeNo = str(params.get("tradeNo"));
        String result = str(params.get("result"));
        String nonce = str(params.get("nonce"));
        String timestamp = str(params.get("timestamp"));
        String amount = str(params.get("amount"));
        String sign = str(params.get("sign"));

        if (payNo.isEmpty() || result.isEmpty() || nonce.isEmpty() || timestamp.isEmpty() || sign.isEmpty()) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }

        // 1. 时间戳窗口：过期回调直接拒绝，防止旧报文重放
        long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (Math.abs(System.currentTimeMillis() - ts) > timestampWindowMs) {
            log.warn("支付回调时间戳超窗：payNo={}, timestamp={}", payNo, timestamp);
            throw new BizException(ResultCode.PAY_NOTIFY_EXPIRED);
        }

        // 2. 验签：参数排序 + key 拼接后 HmacSHA256
        if (!PaymentSignUtil.verify(params, paySecret, sign)) {
            log.warn("支付回调验签失败：payNo={}, params={}", payNo, params);
            throw new BizException(ResultCode.PAY_SIGN_INVALID);
        }

        // 3. nonce 防重放：同一 nonce 只处理一次（Redis 不可用时降级为按支付单状态幂等）
        if (!markNonceOnce(nonce)) {
            log.info("支付回调重放已忽略：payNo={}, nonce={}", payNo, nonce);
            return true;
        }

        PaymentDO payment = paymentMapper.selectByPayNo(payNo);
        if (payment == null) {
            throw new BizException(ResultCode.PAYMENT_NOT_FOUND);
        }

        // 4. 金额校验：渠道金额必须与支付单一致（单位：元）
        if (amount != null && !amount.isEmpty()) {
            try {
                if (new BigDecimal(amount).compareTo(payment.getAmount()) != 0) {
                    log.warn("支付回调金额不一致：payNo={}, 回调={}, 支付单={}", payNo, amount, payment.getAmount());
                    throw new BizException(ResultCode.PAY_AMOUNT_MISMATCH);
                }
            } catch (NumberFormatException e) {
                throw new BizException(ResultCode.PAY_AMOUNT_MISMATCH);
            }
        }

        // 5. 幂等：已终态的支付单回调直接返回成功，避免重复入账
        if (!Objects.equals(payment.getStatus(), Constants.PAY_STATUS_PAYING)) {
            paymentMapper.increaseNotifyCount(payNo);
            log.info("重复回调已去重：payNo={}, status={}, notifyCount+1", payNo, payment.getStatus());
            return true;
        }

        // 6. 支付单已超时：不再入账，直接关闭
        if (payment.getExpireTime() != null && payment.getExpireTime().isBefore(LocalDateTime.now())) {
            paymentMapper.markClosed(payNo, "支付单已超时");
            orderLogService.log(payment.getOrderNo(), OrderAction.PAY_CLOSE,
                    "支付单 " + payNo + " 超过有效期后才收到回调，已关闭未入账", String.valueOf(payment.getUserId()));
            log.warn("支付单超时后才收到回调，已关闭：payNo={}", payNo);
            return true;
        }

        if (RESULT_SUCCESS.equalsIgnoreCase(result)) {
            // 条件更新：并发回调只有一次能把 支付中 -> 成功
            int rows = paymentMapper.markSuccess(payNo, tradeNo);
            if (rows <= 0) {
                paymentMapper.increaseNotifyCount(payNo);
                log.info("并发回调，已由其它线程处理：payNo={}", payNo);
                return true;
            }
            try {
                // 走代理调用，保证 settleSuccess 的事务生效
                applicationContext.getBean(PaymentService.class).settleSuccess(payment);
            } catch (Exception e) {
                // 入账失败：支付单回退为失败，异常抛给渠道（渠道重发时命中幂等）
                paymentMapper.markFailed(payNo, "入账失败：" + e.getMessage());
                log.error("支付入账失败：payNo={}, orderNo={}, msg={}", payNo, payment.getOrderNo(), e.getMessage());
                throw (e instanceof BizException) ? (BizException) e : new BizException(ResultCode.ORDER_PAY_FAILED);
            }
        } else {
            paymentMapper.markFailed(payNo, "渠道返回支付失败：" + result);
            orderLogService.log(payment.getOrderNo(), OrderAction.PAY_FAIL,
                    "支付单 " + payNo + " 渠道返回 " + result + "，订单仍待支付，可重新发起",
                    String.valueOf(payment.getUserId()));
            log.info("支付失败：payNo={}, orderNo={}, result={}", payNo, payment.getOrderNo(), result);
        }
        return true;
    }

    /**
     * 支付成功后的履约：扣钱包 -> 平台入账 -> 订单置为已支付 -> 写时间轴。
     * 与回调处理分事务：回调的状态流转已落库，入账失败会整体回滚并把支付单置为失败。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleSuccess(PaymentDO payment) {
        OrderDO order = orderMapper.selectByOrderNo(payment.getOrderNo());
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (!Objects.equals(order.getStatus(), Constants.ORDER_STATUS_WAIT_PAY)) {
            // 订单已被取消 / 超时 / 已支付：不扣款，支付单标记为失败，留痕供对账
            String text = OrderServiceImpl.statusText(order.getStatus());
            paymentMapper.markFailed(payment.getPayNo(), "订单状态已变更为" + text + "，未扣款");
            orderLogService.log(payment.getOrderNo(), OrderAction.PAY_ABNORMAL,
                    "支付单 " + payment.getPayNo() + " 回调成功，但订单已" + text + "，未扣用户钱包，请人工核对",
                    String.valueOf(order.getUserId()));
            log.warn("支付回调时订单状态异常：payNo={}, orderNo={}, orderStatus={}",
                    payment.getPayNo(), payment.getOrderNo(), order.getStatus());
            return;
        }

        TrainBO train = trainService.getTrainBO(order.getTrainId());
        String title = "购买 " + train.getTrainNo() + " " + TrainServiceImpl.seatTypeName(order.getSeatType());
        String detail = train.getFromStationName() + " -> " + train.getToStationName()
                + " " + order.getCarriageNo() + "车" + order.getSeatNo() + "座 · " + order.getPassengerName();

        // 钱包扣款（余额不足抛异常 -> 事务回滚 -> 支付单置失败）
        BigDecimal balance = walletService.pay(order.getUserId(), order.getPrice(), payment.getOrderNo(), title, detail);
        // 平台收款账户同步入账：用户扣款 -> 平台收款，形成资金闭环
        creditPlatform(payment.getOrderNo(), train.getTrainNo(), order.getPrice(), detail);

        int rows = orderMapper.markPaid(payment.getOrderNo(), LocalDateTime.now());
        if (rows <= 0) {
            throw new BizException(ResultCode.ORDER_PAY_FAILED);
        }
        orderLogService.log(payment.getOrderNo(), OrderAction.PAY,
                "支付单 " + payment.getPayNo() + " 扣款 " + order.getPrice() + " 元，钱包余额 " + balance,
                String.valueOf(order.getUserId()));
        log.info("支付回调入账成功：payNo={}, orderNo={}, 扣款={}, 钱包余额={}",
                payment.getPayNo(), payment.getOrderNo(), order.getPrice(), balance);
    }

    @Override
    public boolean mockCallback(String payNo, String result) {
        PaymentDO payment = paymentMapper.selectByPayNo(payNo);
        if (payment == null) {
            throw new BizException(ResultCode.PAYMENT_NOT_FOUND);
        }
        Map<String, Object> params = new HashMap<>(8);
        params.put("payNo", payNo);
        params.put("tradeNo", "MOCK" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999));
        params.put("result", (result == null || result.trim().isEmpty()) ? RESULT_SUCCESS : result.trim().toUpperCase());
        params.put("amount", payment.getAmount().toPlainString());
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        params.put("nonce", java.util.UUID.randomUUID().toString().replace("-", ""));
        // 按约定生成签名，回调处理里正常验签（与真实渠道一致）
        params.put("sign", PaymentSignUtil.sign(params, paySecret));
        log.info("模拟渠道发起回调：payNo={}, result={}", payNo, params.get("result"));
        return handleNotify(params);
    }

    @Override
    public void closeByOrderNo(String orderNo, String reason) {
        try {
            int rows = paymentMapper.closeByOrderNo(orderNo, reason);
            if (rows > 0) {
                // 与关单同事务：取消 / 超时关单后支付单被关闭，后续回调命中幂等不会误入账
                orderLogService.log(orderNo, OrderAction.PAY_CLOSE,
                        "关闭支付单 " + rows + " 笔，原因：" + reason + "，后续回调不会入账", "system");
                log.info("关闭支付单：orderNo={}, 笔数={}, 原因={}", orderNo, rows, reason);
            }
        } catch (Exception e) {
            log.warn("关闭支付单失败：orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }

    // ==================== private ====================

    /**
     * nonce 写入 Redis（SET NX）：成功说明首次回调，false 说明已处理过。
     * Redis 不可用时返回 true，降级为「按支付单状态幂等」。
     */
    private boolean markNonceOnce(String nonce) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(RedisKeys.payNotifyNonce(nonce), "1", nonceTtlSeconds, TimeUnit.SECONDS);
            return !Boolean.FALSE.equals(ok);
        } catch (Exception e) {
            log.warn("nonce 防重放写入 Redis 失败，降级为按支付单状态幂等：msg={}", e.getMessage());
            return true;
        }
    }

    /**
     * 事务提交后再调度模拟回调，避免回调先到、支付单还没落库。
     */
    private void scheduleMockCallback(String payNo, String result) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSchedule(payNo, result);
                }
            });
        } else {
            doSchedule(payNo, result);
        }
    }

    private void doSchedule(String payNo, String result) {
        try {
            callbackScheduler.schedule(() -> {
                try {
                    mockCallback(payNo, result);
                } catch (Exception e) {
                    log.error("模拟渠道回调执行失败：payNo={}, msg={}", payNo, e.getMessage());
                }
            }, mockCallbackDelayMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("调度模拟回调失败：payNo={}, msg={}", payNo, e.getMessage());
        }
    }

    private String generatePayNo(Long userId) {
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        String userTail = String.format("%04d", (userId == null ? 0 : userId) % 10000);
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "PAY" + time + userTail + rand;
    }

    private PaymentVO toVO(PaymentDO payment) {
        return PaymentVO.builder()
                .payNo(payment.getPayNo())
                .orderNo(payment.getOrderNo())
                .userId(payment.getUserId())
                .payType(payment.getPayType())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .statusText(Constants.payStatusText(payment.getStatus()))
                .tradeNo(payment.getTradeNo())
                .notifyCount(payment.getNotifyCount())
                .expireTime(payment.getExpireTime())
                .payTime(payment.getPayTime())
                .createTime(payment.getCreateTime())
                .failReason(payment.getFailReason())
                .build();
    }

    /**
     * 平台收款：票款进入平台账户钱包（失败不影响支付主流程，只记录日志）。
     */
    private void creditPlatform(String orderNo, String trainNo, BigDecimal amount, String detail) {
        try {
            walletService.creditToPlatform(Constants.PLATFORM_USER_ID, amount, orderNo,
                    "售票收入 " + trainNo, detail);
        } catch (Exception e) {
            log.warn("平台账户入账失败（不影响支付）：orderNo={}, msg={}", orderNo, e.getMessage());
        }
    }



    private String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}

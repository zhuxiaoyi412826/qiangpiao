package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.common.util.OrderNoGenerator;
import com.qiangpiao.dataobject.WalletDO;
import com.qiangpiao.dataobject.WalletFlowDO;
import com.qiangpiao.dto.RechargeDTO;
import com.qiangpiao.mapper.WalletFlowMapper;
import com.qiangpiao.mapper.WalletMapper;
import com.qiangpiao.service.WalletService;
import com.qiangpiao.vo.WalletFlowVO;
import com.qiangpiao.vo.WalletVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钱包服务实现：
 * <pre>
 *   充值：余额自增 + total_recharge 累加 + 写流水（幂等由业务层控制）
 *   消费：乐观锁 + SQL balance >= amount 双重保证（防并发扣成负数），写流水
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    /** 乐观锁重试次数 */
    private static final int RETRY_TIMES = 3;
    /** 单笔充值上限 */
    private static final BigDecimal MAX_RECHARGE_AMOUNT = new BigDecimal("50000");
    /** 金额保留小数位 */
    private static final int SCALE = 2;

    private final WalletMapper walletMapper;
    private final WalletFlowMapper walletFlowMapper;

    @Override
    public WalletVO getWallet(Long userId) {
        return toVO(getOrCreate(userId));
    }

    /**
     * 注册礼包：先幂等查流水（并发下由 idempotent_key 唯一索引兜底），再开户 + 入账 + 记一笔「充值」流水。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletVO grantRegisterGift(Long userId, BigDecimal giftAmount) {
        BigDecimal gift = normalize(giftAmount);
        if (userId == null) {
            throw new BizException(ResultCode.BAD_REQUEST);
        }
        if (gift.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("注册礼包配置为 0，仅开通钱包：userId={}", userId);
            return getWallet(userId);
        }

        String idempotentKey = Constants.WALLET_GIFT_IDEMPOTENT_PREFIX + userId;
        WalletFlowDO exist = walletFlowMapper.selectByIdempotentKey(idempotentKey);
        if (exist != null) {
            log.info("注册礼包已发放，跳过重复赠送：userId={}, amount={}", userId, exist.getAmount());
            WalletDO wallet = walletMapper.selectByUserId(userId);
            return wallet == null ? getWallet(userId) : toVO(wallet);
        }

        getOrCreate(userId);
        walletMapper.increaseBalance(userId, gift);
        WalletDO updated = walletMapper.selectByUserId(userId);
        insertFlow(userId, Constants.WALLET_GIFT_BIZ_NO_PREFIX + userId, Constants.WALLET_FLOW_RECHARGE,
                "开户赠送", "新用户注册赠送初始余额 " + gift.toPlainString() + " 元",
                gift, updated.getBalance(), "注册自动开通钱包并赠送", idempotentKey);

        log.info("注册礼包发放成功：userId={}, gift={}, balance={}", userId, gift, updated.getBalance());
        return toVO(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletVO recharge(Long userId, RechargeDTO dto) {
        BigDecimal amount = normalize(dto.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.WALLET_AMOUNT_INVALID);
        }
        if (amount.compareTo(MAX_RECHARGE_AMOUNT) > 0) {
            throw new BizException(ResultCode.WALLET_RECHARGE_LIMIT);
        }

        getOrCreate(userId);
        if (walletMapper.increaseBalance(userId, amount) <= 0) {
            throw new BizException(ResultCode.WALLET_OPERATE_FAILED);
        }
        WalletDO updated = walletMapper.selectByUserId(userId);

        String flowNo = OrderNoGenerator.generate(userId);
        insertFlow(userId, flowNo, Constants.WALLET_FLOW_RECHARGE, "余额充值",
                "自定义充值 " + amount.toPlainString() + " 元", amount, updated.getBalance(), dto.getRemark());

        log.info("钱包充值成功：userId={}, flowNo={}, amount={}, balance={}",
                userId, flowNo, amount, updated.getBalance());
        return toVO(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal pay(Long userId, BigDecimal amount, String bizNo, String title, String detail) {
        BigDecimal cost = normalize(amount);
        WalletDO wallet = getOrCreate(userId);
        if (wallet.getBalance().compareTo(cost) < 0) {
            throw new BizException(ResultCode.WALLET_BALANCE_NOT_ENOUGH);
        }
        decreaseWithRetry(userId, cost);

        WalletDO updated = walletMapper.selectByUserId(userId);
        insertFlow(userId, bizNo, Constants.WALLET_FLOW_CONSUME, title, detail,
                cost.negate(), updated.getBalance(), null);

        log.info("钱包扣款成功：userId={}, bizNo={}, amount={}, balance={}",
                userId, bizNo, cost, updated.getBalance());
        return updated.getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal refund(Long userId, BigDecimal amount, String bizNo, String title, String detail) {
        BigDecimal back = normalize(amount);
        getOrCreate(userId);
        walletMapper.increaseBalance(userId, back);
        WalletDO updated = walletMapper.selectByUserId(userId);
        insertFlow(userId, bizNo, Constants.WALLET_FLOW_REFUND, title, detail, back, updated.getBalance(), null, null);
        log.info("退票退款入账：userId={}, bizNo={}, amount={}, balance={}",
                userId, bizNo, back, updated.getBalance());
        return updated.getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal refundOnce(Long userId, BigDecimal amount, String idempotentKey,
                                String bizNo, String title, String detail) {
        // 幂等第一道：流水已存在说明这笔账已退过，直接返回当时余额快照
        if (org.springframework.util.StringUtils.hasText(idempotentKey)) {
            WalletFlowDO exist = walletFlowMapper.selectByIdempotentKey(idempotentKey);
            if (exist != null) {
                log.info("退款幂等命中，跳过重复入账：key={}, flowNo={}, amount={}",
                        idempotentKey, exist.getFlowNo(), exist.getAmount());
                return exist.getBalance();
            }
        }
        BigDecimal back = normalize(amount);
        getOrCreate(userId);
        walletMapper.increaseBalance(userId, back);
        WalletDO updated = walletMapper.selectByUserId(userId);
        // 幂等第二道：并发下由 t_wallet_flow.idempotent_key 唯一索引兜底，
        // 后到的请求抛 DuplicateKeyException → 当前事务回滚，余额不会多加
        insertFlow(userId, bizNo, Constants.WALLET_FLOW_REFUND, title, detail, back,
                updated.getBalance(), null, idempotentKey);
        log.info("退票退款入账（幂等）：userId={}, bizNo={}, key={}, amount={}, balance={}",
                userId, bizNo, idempotentKey, back, updated.getBalance());
        return updated.getBalance();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BigDecimal creditToPlatform(Long userId, BigDecimal amount, String bizNo, String title, String detail) {
        BigDecimal income = normalize(amount);
        getOrCreate(userId);
        walletMapper.increaseBalance(userId, income);
        WalletDO updated = walletMapper.selectByUserId(userId);
        insertFlow(userId, bizNo, Constants.WALLET_FLOW_PLATFORM_INCOME, title, detail,
                income, updated.getBalance(), null);
        log.info("平台账户收款：platformUserId={}, bizNo={}, amount={}, balance={}",
                userId, bizNo, income, updated.getBalance());
        return updated.getBalance();
    }

    @Override
    public PageResult<WalletFlowVO> pageFlows(Long userId, Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 50);
        long offset = (long) (pn - 1) * ps;

        List<WalletFlowDO> flows = walletFlowMapper.selectByUserId(userId, offset, (long) ps);
        long total = walletFlowMapper.countByUserId(userId);
        List<WalletFlowVO> list = flows.stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(pn, ps, total, list);
    }

    // ==================== private ====================

    /**
     * 获取钱包，首次使用时自动开户。
     */
    private WalletDO getOrCreate(Long userId) {
        WalletDO wallet = walletMapper.selectByUserId(userId);
        if (wallet != null) {
            return wallet;
        }
        walletMapper.insertIgnore(userId);
        wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            throw new BizException(ResultCode.WALLET_NOT_FOUND);
        }
        return wallet;
    }

    /**
     * 乐观锁扣款：version 冲突或余额不足时重试，最终失败抛业务异常（由上层事务回滚）。
     */
    private void decreaseWithRetry(Long userId, BigDecimal amount) {
        for (int i = 0; i < RETRY_TIMES; i++) {
            WalletDO wallet = walletMapper.selectByUserId(userId);
            if (wallet == null) {
                throw new BizException(ResultCode.WALLET_NOT_FOUND);
            }
            if (wallet.getBalance().compareTo(amount) < 0) {
                throw new BizException(ResultCode.WALLET_BALANCE_NOT_ENOUGH);
            }
            if (walletMapper.decreaseBalance(userId, amount, wallet.getVersion()) > 0) {
                return;
            }
        }
        throw new BizException(ResultCode.WALLET_OPERATE_FAILED);
    }

    private void insertFlow(Long userId, String bizNo, Integer type, String title, String detail,
                            BigDecimal amount, BigDecimal balance, String remark) {
        insertFlow(userId, bizNo, type, title, detail, amount, balance, remark, null);
    }

    private void insertFlow(Long userId, String bizNo, Integer type, String title, String detail,
                            BigDecimal amount, BigDecimal balance, String remark, String idempotentKey) {
        WalletFlowDO flow = new WalletFlowDO();
        flow.setFlowNo(OrderNoGenerator.generate(userId));
        flow.setUserId(userId);
        flow.setBizNo(bizNo);
        flow.setType(type);
        flow.setTitle(title);
        flow.setDetail(detail);
        flow.setAmount(normalize(amount));
        flow.setBalance(normalize(balance));
        flow.setRemark(remark);
        flow.setIdempotentKey(idempotentKey);
        walletFlowMapper.insert(flow);
    }

    private BigDecimal normalize(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    private WalletVO toVO(WalletDO wallet) {
        return WalletVO.builder()
                .userId(wallet.getUserId())
                .balance(wallet.getBalance())
                .totalRecharge(wallet.getTotalRecharge())
                .totalConsume(wallet.getTotalConsume())
                .build();
    }

    private WalletFlowVO toVO(WalletFlowDO flow) {
        return WalletFlowVO.builder()
                .id(flow.getId())
                .flowNo(flow.getFlowNo())
                .bizNo(flow.getBizNo())
                .type(flow.getType())
                .typeText(Constants.walletFlowTypeName(flow.getType()))
                .title(flow.getTitle())
                .detail(flow.getDetail())
                .amount(flow.getAmount())
                .balance(flow.getBalance())
                .remark(flow.getRemark())
                .createTime(flow.getCreateTime())
                .build();
    }
}

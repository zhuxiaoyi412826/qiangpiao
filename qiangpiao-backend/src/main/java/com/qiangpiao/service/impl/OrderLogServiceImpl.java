package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.OrderAction;
import com.qiangpiao.common.util.TraceContext;
import com.qiangpiao.dataobject.OrderLogDO;
import com.qiangpiao.mapper.OrderLogMapper;
import com.qiangpiao.service.OrderLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 订单流转日志实现。
 * <p>
 * 写入失败只告警不影响业务：日志是旁路数据，不能因为写不进去就让下单 / 支付失败。
 */
@Slf4j
@Service
public class OrderLogServiceImpl implements OrderLogService {

    private final OrderLogMapper orderLogMapper;
    private final Executor asyncExecutor;

    public OrderLogServiceImpl(OrderLogMapper orderLogMapper,
                               @Qualifier("seckillExecutor") Executor asyncExecutor) {
        this.orderLogMapper = orderLogMapper;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void log(String orderNo, OrderAction action, String detail, String operator) {
        doInsert(orderNo, action, detail, operator, TraceContext.traceId());
    }

    @Override
    public void logAsync(String orderNo, OrderAction action, String detail, String operator) {
        // MDC 是线程私有的，异步线程里先恢复 traceId，保证日志可串联
        String traceId = TraceContext.traceId();
        try {
            asyncExecutor.execute(() -> {
                try {
                    TraceContext.putTraceId(traceId);
                    doInsert(orderNo, action, detail, operator, traceId);
                } finally {
                    TraceContext.clear();
                }
            });
        } catch (Exception e) {
            // 线程池打满时退化成同步写，宁可慢一点也不能丢日志
            log.warn("订单日志异步提交失败，改为同步写入：orderNo={}, msg={}", orderNo, e.getMessage());
            doInsert(orderNo, action, detail, operator, traceId);
        }
    }

    @Override
    public List<OrderLogDO> timeline(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return Collections.emptyList();
        }
        return orderLogMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<OrderLogDO> timelines(List<String> orderNos) {
        if (orderNos == null || orderNos.isEmpty()) {
            return Collections.emptyList();
        }
        return orderLogMapper.selectByOrderNos(orderNos);
    }

    private void doInsert(String orderNo, OrderAction action, String detail, String operator, String traceId) {
        if (!StringUtils.hasText(orderNo) || action == null) {
            return;
        }
        try {
            OrderLogDO logDO = new OrderLogDO();
            logDO.setOrderNo(orderNo);
            logDO.setAction(action.name());
            logDO.setActionText(action.getText());
            logDO.setDetail(detail);
            logDO.setOperator(operator);
            logDO.setTraceId(traceId);
            orderLogMapper.insert(logDO);
        } catch (Exception e) {
            log.warn("写入订单流转日志失败：orderNo={}, action={}, msg={}", orderNo, action, e.getMessage());
        }
    }
}

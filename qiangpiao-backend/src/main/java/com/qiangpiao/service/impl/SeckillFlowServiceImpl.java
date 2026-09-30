package com.qiangpiao.service.impl;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dataobject.SeckillFlowDO;
import com.qiangpiao.mapper.SeckillFlowMapper;
import com.qiangpiao.service.SeckillFlowService;
import com.qiangpiao.vo.SeckillFlowVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 抢票流水实现。
 * <p>
 * 写入全部走线程池异步执行，且吞掉异常：流水是旁路数据，
 * 绝不能因为写不进去就让抢票失败或变慢。
 */
@Slf4j
@Service
public class SeckillFlowServiceImpl implements SeckillFlowService {

    private final SeckillFlowMapper seckillFlowMapper;
    private final Executor asyncExecutor;

    public SeckillFlowServiceImpl(SeckillFlowMapper seckillFlowMapper,
                                  @Qualifier("seckillExecutor") Executor asyncExecutor) {
        this.seckillFlowMapper = seckillFlowMapper;
        this.asyncExecutor = asyncExecutor;
    }

    @Override
    public void accept(SeckillFlowDO flow) {
        if (flow == null || !StringUtils.hasText(flow.getOrderNo())) {
            return;
        }
        if (flow.getStatus() == null) {
            flow.setStatus(SeckillFlowVO.STATUS_QUEUEING);
        }
        async(() -> {
            try {
                seckillFlowMapper.insert(flow);
            } catch (Exception e) {
                log.warn("写入抢票流水失败：orderNo={}, msg={}", flow.getOrderNo(), e.getMessage());
            }
        });
    }

    @Override
    public void finish(String orderNo, int status, String failReason, Long costMs) {
        if (!StringUtils.hasText(orderNo)) {
            return;
        }
        async(() -> {
            try {
                int rows = seckillFlowMapper.updateResult(orderNo, status, failReason, costMs);
                if (rows <= 0) {
                    // 异步时序下受理流水可能还没落库：补一条带终态的记录，保证不丢流水
                    SeckillFlowDO flow = new SeckillFlowDO();
                    flow.setOrderNo(orderNo);
                    flow.setStatus(status);
                    flow.setFailReason(failReason);
                    flow.setCostMs(costMs);
                    seckillFlowMapper.insert(flow);
                }
            } catch (Exception e) {
                log.warn("回写抢票流水结果失败：orderNo={}, msg={}", orderNo, e.getMessage());
            }
        });
    }

    @Override
    public List<SeckillFlowVO> myFlows(Long userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int size = limit <= 0 ? 20 : Math.min(limit, 100);
        return seckillFlowMapper.selectByUser(userId, (long) size).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<SeckillFlowVO> batchFlows(String batchNo) {
        return listByBatch(batchNo).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<SeckillFlowDO> listByBatch(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return Collections.emptyList();
        }
        return seckillFlowMapper.selectByBatch(batchNo);
    }

    @Override
    public PageResult<SeckillFlowVO> page(Long userId, Long trainId, Integer status, Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1) ? 10 : Math.min(pageSize, 100);
        long offset = (long) (pn - 1) * ps;
        List<SeckillFlowVO> list = seckillFlowMapper.selectPage(userId, trainId, status, offset, (long) ps)
                .stream().map(this::toVO).collect(Collectors.toList());
        long total = seckillFlowMapper.countPage(userId, trainId, status);
        return PageResult.of(pn, ps, total, list);
    }

    private void async(Runnable task) {
        try {
            asyncExecutor.execute(task);
        } catch (Exception e) {
            // 线程池打满：直接同步执行，慢一点也不能丢流水
            task.run();
        }
    }

    private SeckillFlowVO toVO(SeckillFlowDO flow) {
        return SeckillFlowVO.builder()
                .id(flow.getId())
                .batchNo(flow.getBatchNo())
                .orderNo(flow.getOrderNo())
                .userId(flow.getUserId())
                .trainId(flow.getTrainId())
                .seatType(flow.getSeatType())
                .passengerName(flow.getPassengerName())
                .ticketIndex(flow.getTicketIndex())
                .status(flow.getStatus())
                .statusText(statusText(flow.getStatus()))
                .failReason(flow.getFailReason())
                .queueSeq(flow.getQueueSeq())
                .costMs(flow.getCostMs())
                .createTime(flow.getCreateTime())
                .finishTime(flow.getFinishTime())
                .build();
    }

    private String statusText(Integer status) {
        if (status == null) {
            return "排队中";
        }
        switch (status) {
            case SeckillFlowVO.STATUS_SUCCESS:
                return "抢票成功";
            case SeckillFlowVO.STATUS_FAILED:
                return "抢票失败";
            default:
                return "排队中";
        }
    }
}

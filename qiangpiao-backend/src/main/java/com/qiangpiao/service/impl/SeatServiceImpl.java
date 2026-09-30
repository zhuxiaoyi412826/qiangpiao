package com.qiangpiao.service.impl;

import com.qiangpiao.bo.RangeBO;
import com.qiangpiao.bo.SeatBO;
import com.qiangpiao.cache.MultiLevelCacheService;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.dataobject.SeatSegmentDO;
import com.qiangpiao.mapper.SeatMapper;
import com.qiangpiao.mapper.SeatSegmentMapper;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.service.SegmentStockService;
import com.qiangpiao.vo.SeatMapVO;
import com.qiangpiao.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 座位服务实现：座位图三级缓存 + 乐观锁抢占座位 + 区间复用。
 *
 * <pre>
 *   区间票模式（建表后）：同一座位可以被拆成多段卖给不同乘客（A-B 与 B-C 共用一座），
 *     冲突由 t_seat_segment 判定：已有 [a,b) 与新 [fo,to) 冲突 ⇔ a &lt; to 且 fo &lt; b；
 *     t_seat.status 表示「该座位是否还有任何有效占用」，最后一个区间释放后才置回可售。
 *   全程票模式（未建表）：沿用原来的 status=0 抢占，行为完全不变。
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    /** 抢占座位重试次数 */
    private static final int LOCK_RETRY = 3;

    private final SeatMapper seatMapper;
    private final SeatSegmentMapper seatSegmentMapper;
    private final SegmentStockService segmentStockService;
    private final MultiLevelCacheService cacheService;

    @Override
    public SeatMapVO seatMap(Long trainId, Integer seatType) {
        SeatMapVO map = cacheService.get(RedisKeys.seatMap(trainId, seatType), SeatMapVO.class,
                () -> loadSeatMap(trainId, seatType), 120L);
        if (map == null) {
            throw new BizException(ResultCode.SEAT_NOT_FOUND);
        }
        return map;
    }

    /**
     * 按乘车区间取座位图：区间被占用的座位标为不可选，未占用的（哪怕其它区间已售）仍可选。
     */
    @Override
    public SeatMapVO seatMap(Long trainId, Integer seatType, RangeBO range) {
        if (!segmentStockService.enabled() || range == null) {
            return seatMap(trainId, seatType);
        }
        List<SeatDO> seats = seatMapper.selectByTrainAndType(trainId, seatType);
        if (seats == null || seats.isEmpty()) {
            throw new BizException(ResultCode.SEAT_NOT_FOUND);
        }
        List<SeatVO> vos = seats.stream().map(seat -> {
            boolean conflict = seatSegmentMapper.countConflict(seat.getId(),
                    range.getFromOrder(), range.getToOrder()) > 0;
            return SeatVO.builder()
                    .seatId(seat.getId())
                    .carriageNo(seat.getCarriageNo())
                    .seatNo(seat.getSeatNo())
                    .seatType(seat.getSeatType())
                    .status(conflict ? 1 : 0)
                    .build();
        }).collect(Collectors.toList());
        long available = vos.stream().filter(v -> v.getStatus() == 0).count();
        return SeatMapVO.builder()
                .trainId(trainId)
                .seatType(seatType)
                .totalCount(vos.size())
                .availableCount((int) available)
                .seats(vos)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo) {
        return pickAndLockSeat(trainId, seatType, seatId, orderNo, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo,
                                  Integer preferCarriageNo) {
        return pickAndLockSeat(trainId, seatType, seatId, orderNo, preferCarriageNo, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo,
                                  Integer preferCarriageNo, RangeBO range) {
        RangeBO safeRange = safeRange(range);
        boolean segmentMode = segmentStockService.enabled();

        if (seatId != null) {
            return lockSpecified(trainId, seatType, seatId, orderNo, safeRange, segmentMode);
        }
        return lockAuto(trainId, seatType, orderNo, preferCarriageNo, safeRange, segmentMode);
    }

    /** 指定座位下单 */
    private SeatBO lockSpecified(Long trainId, Integer seatType, Long seatId, String orderNo,
                                 RangeBO range, boolean segmentMode) {
        SeatDO seat = segmentMode ? seatMapper.selectByIdForUpdate(seatId) : seatMapper.selectById(seatId);
        if (seat == null) {
            throw new BizException(ResultCode.SEAT_NOT_FOUND);
        }
        if (segmentMode) {
            // 区间冲突判定：该座位在 [from, to) 内必须完全空闲
            if (seatSegmentMapper.countConflict(seatId, range.getFromOrder(), range.getToOrder()) > 0) {
                throw new BizException(ResultCode.SEAT_SOLD);
            }
            seatMapper.occupySeat(seatId, orderNo);
            insertSegment(trainId, seatId, seatType, orderNo, range);
        } else {
            if (seat.getStatus() == null || seat.getStatus() != 0) {
                throw new BizException(ResultCode.SEAT_SOLD);
            }
            int rows = seatMapper.lockSeatById(seatId, orderNo);
            if (rows <= 0) {
                throw new BizException(ResultCode.SEAT_SOLD);
            }
        }
        log.info("指定占座成功：seatId={}, orderNo={}, 区间=[{},{}), 模式={}",
                seatId, orderNo, range.getFromOrder(), range.getToOrder(), segmentMode ? "区间" : "全程");
        seat.setStatus(1);
        return toBO(seat);
    }

    /** 自动分配座位（同批次优先同车厢） */
    private SeatBO lockAuto(Long trainId, Integer seatType, String orderNo, Integer preferCarriageNo,
                            RangeBO range, boolean segmentMode) {
        SeatDO seat;
        if (segmentMode) {
            // 区间模式：只排除「本区间已被占用」的座位，其它区间占用不影响本次购买
            seat = preferCarriageNo == null ? null
                    : seatSegmentMapper.selectFreeForUpdate(trainId, seatType,
                    range.getFromOrder(), range.getToOrder(), preferCarriageNo);
            if (seat == null) {
                seat = seatSegmentMapper.selectFreeForUpdate(trainId, seatType,
                        range.getFromOrder(), range.getToOrder(), null);
            }
            if (seat == null) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
            seatMapper.occupySeat(seat.getId(), orderNo);
            insertSegment(trainId, seat.getId(), seatType, orderNo, range);
        } else {
            seat = null;
            for (int i = 0; i < LOCK_RETRY && seat == null; i++) {
                if (preferCarriageNo != null) {
                    seat = seatMapper.selectAvailableForUpdate(trainId, seatType, preferCarriageNo);
                }
                if (seat == null) {
                    seat = seatMapper.selectAvailableForUpdate(trainId, seatType, null);
                }
            }
            if (seat == null) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
            int rows = seatMapper.lockSeatById(seat.getId(), orderNo);
            if (rows <= 0) {
                throw new BizException(ResultCode.SEAT_SOLD);
            }
        }
        log.info("自动分配座位成功：trainId={}, seatId={}, {}车{}座, preferCarriage={}, 区间=[{},{}), orderNo={}",
                trainId, seat.getId(), seat.getCarriageNo(), seat.getSeatNo(), preferCarriageNo,
                range.getFromOrder(), range.getToOrder(), orderNo);
        seat.setStatus(1);
        return toBO(seat);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeat(String orderNo) {
        if (segmentStockService.enabled()) {
            List<SeatSegmentDO> segments = seatSegmentMapper.selectByOrderNo(orderNo);
            seatSegmentMapper.releaseByOrderNo(orderNo);
            for (SeatSegmentDO segment : segments) {
                // 该座位的区间全部释放后，座位才真正回到可售状态（区间复用：其它订单可能还占着别的段）
                if (seatSegmentMapper.countActiveBySeat(segment.getSeatId()) == 0) {
                    seatMapper.freeSeat(segment.getSeatId());
                }
            }
            // 老数据（建表前的订单没有区间记录）仍按订单号释放
            if (segments.isEmpty()) {
                seatMapper.releaseSeatByOrderNo(orderNo);
            }
            log.info("释放座位区间：orderNo={}, 释放段数={}", orderNo, segments.size());
            return;
        }
        int rows = seatMapper.releaseSeatByOrderNo(orderNo);
        log.info("释放座位：orderNo={}, rows={}", orderNo, rows);
    }

    @Override
    public void evictSeatCache(Long trainId, Integer seatType) {
        cacheService.evict(RedisKeys.seatMap(trainId, seatType));
    }

    private void insertSegment(Long trainId, Long seatId, Integer seatType, String orderNo, RangeBO range) {
        SeatSegmentDO segment = new SeatSegmentDO();
        segment.setTrainId(trainId);
        segment.setSeatId(seatId);
        segment.setSeatType(seatType);
        segment.setFromOrder(range.getFromOrder());
        segment.setToOrder(range.getToOrder());
        segment.setOrderNo(orderNo);
        segment.setStatus(1);
        seatSegmentMapper.insert(segment);
    }

    private RangeBO safeRange(RangeBO range) {
        if (range != null && range.getFromOrder() != null && range.getToOrder() != null) {
            return range;
        }
        return RangeBO.builder()
                .fromOrder(RangeBO.DEFAULT_FROM)
                .toOrder(RangeBO.DEFAULT_TO)
                .build();
    }

    private SeatMapVO loadSeatMap(Long trainId, Integer seatType) {
        List<SeatDO> seats = seatMapper.selectByTrainAndType(trainId, seatType);
        if (seats == null || seats.isEmpty()) {
            return null;
        }
        List<SeatVO> vos = seats.stream().map(seat -> SeatVO.builder()
                .seatId(seat.getId())
                .carriageNo(seat.getCarriageNo())
                .seatNo(seat.getSeatNo())
                .seatType(seat.getSeatType())
                .status(seat.getStatus())
                .build()).collect(Collectors.toList());
        long available = vos.stream().filter(v -> v.getStatus() != null && v.getStatus() == 0).count();
        return SeatMapVO.builder()
                .trainId(trainId)
                .seatType(seatType)
                .totalCount(vos.size())
                .availableCount((int) available)
                .seats(vos)
                .build();
    }

    private SeatBO toBO(SeatDO seat) {
        return SeatBO.builder()
                .seatId(seat.getId())
                .trainId(seat.getTrainId())
                .seatType(seat.getSeatType())
                .carriageNo(seat.getCarriageNo())
                .seatNo(seat.getSeatNo())
                .status(seat.getStatus())
                .build();
    }
}

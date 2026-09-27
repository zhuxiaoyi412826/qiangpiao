package com.qiangpiao.service.impl;

import com.qiangpiao.bo.SeatBO;
import com.qiangpiao.cache.MultiLevelCacheService;
import com.qiangpiao.common.constant.RedisKeys;
import com.qiangpiao.common.exception.BizException;
import com.qiangpiao.common.result.ResultCode;
import com.qiangpiao.dataobject.SeatDO;
import com.qiangpiao.mapper.SeatMapper;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.vo.SeatMapVO;
import com.qiangpiao.vo.SeatVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 座位服务实现：座位图三级缓存 + 乐观锁抢占座位。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    /** 抢占座位重试次数 */
    private static final int LOCK_RETRY = 3;

    private final SeatMapper seatMapper;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeatBO pickAndLockSeat(Long trainId, Integer seatType, Long seatId, String orderNo) {
        if (seatId != null) {
            SeatDO seat = seatMapper.selectById(seatId);
            if (seat == null || seat.getStatus() == null || seat.getStatus() != 0) {
                throw new BizException(ResultCode.SEAT_SOLD);
            }
            int rows = seatMapper.lockSeatById(seatId, orderNo);
            if (rows <= 0) {
                throw new BizException(ResultCode.SEAT_SOLD);
            }
            log.info("指定占座成功：seatId={}, orderNo={}", seatId, orderNo);
            return toBO(seat);
        }

        for (int i = 0; i < LOCK_RETRY; i++) {
            SeatDO seat = seatMapper.selectAvailableOne(trainId, seatType);
            if (seat == null) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
            int rows = seatMapper.lockSeat(seat.getId(), seat.getVersion(), orderNo);
            if (rows > 0) {
                log.info("自动分配座位成功：trainId={}, seatId={}, orderNo={}", trainId, seat.getId(), orderNo);
                return toBO(seat);
            }
        }
        throw new BizException(ResultCode.SEAT_SOLD);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSeat(String orderNo) {
        int rows = seatMapper.releaseSeatByOrderNo(orderNo);
        log.info("释放座位：orderNo={}, rows={}", orderNo, rows);
    }

    @Override
    public void evictSeatCache(Long trainId, Integer seatType) {
        cacheService.evict(RedisKeys.seatMap(trainId, seatType));
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

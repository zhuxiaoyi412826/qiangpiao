package com.qiangpiao.service.impl;

import com.qiangpiao.common.constant.Constants;
import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.dataobject.TicketDO;
import com.qiangpiao.dto.TicketQueryDTO;
import com.qiangpiao.mapper.TicketMapper;
import com.qiangpiao.service.TicketService;
import com.qiangpiao.vo.TicketVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 车票服务实现：
 * <pre>
 *   未开车：已支付(status=1) 且 发车时刻 &gt; now
 *   历史  ：已发车的票 + 已取消/退票/超时的订单
 *   待支付订单不算车票，仍需到「我的订单」支付后再进「我的车票」
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TicketMapper ticketMapper;

    @Override
    public PageResult<TicketVO> pageTickets(Long userId, TicketQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum() == null || queryDTO.getPageNum() < 1 ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null || queryDTO.getPageSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(queryDTO.getPageSize(), MAX_PAGE_SIZE);
        boolean history = queryDTO.isHistory();
        long offset = (long) (pageNum - 1) * pageSize;

        List<TicketDO> tickets = ticketMapper.selectTickets(userId, history, offset, (long) pageSize);
        long total = ticketMapper.countTickets(userId, history);

        LocalDateTime now = LocalDateTime.now();
        List<TicketVO> list = tickets.stream()
                .map(ticket -> toVO(ticket, now))
                .collect(Collectors.toList());
        return PageResult.of(pageNum, pageSize, total, list);
    }

    private TicketVO toVO(TicketDO ticket, LocalDateTime now) {
        LocalDate departDate = ticket.getDepartDate() == null ? now.toLocalDate() : ticket.getDepartDate();
        LocalTime departTime = ticket.getDepartTime() == null ? LocalTime.MIDNIGHT : ticket.getDepartTime();
        LocalDateTime departAt = LocalDateTime.of(departDate, departTime);

        boolean effective = Integer.valueOf(Constants.ORDER_STATUS_PAID).equals(ticket.getStatus());
        boolean departed = !departAt.isAfter(now);

        int ticketStatus;
        String ticketStatusText;
        if (!effective) {
            ticketStatus = 3;
            ticketStatusText = "已失效";
        } else if (departed) {
            ticketStatus = 2;
            ticketStatusText = "已出行";
        } else {
            ticketStatus = 1;
            ticketStatusText = "待出行";
        }

        return TicketVO.builder()
                .id(ticket.getId())
                .orderNo(ticket.getOrderNo())
                .trainNo(ticket.getTrainNo())
                .trainType(ticket.getTrainType())
                .fromStationName(ticket.getFromStationName())
                .toStationName(ticket.getToStationName())
                .departDate(departDate.toString())
                .departTime(departTime.toString())
                .arriveTime(ticket.getArriveTime() == null ? null : ticket.getArriveTime().toString())
                .durationText(durationText(ticket.getDurationMinutes()))
                .seatTypeName(TrainServiceImpl.seatTypeName(ticket.getSeatType()))
                .carriageNo(ticket.getCarriageNo())
                .seatNo(ticket.getSeatNo())
                .passengerName(ticket.getPassengerName())
                .price(ticket.getPrice())
                .status(ticket.getStatus())
                .statusText(statusText(ticket.getStatus()))
                .ticketStatus(ticketStatus)
                .ticketStatusText(ticketStatusText)
                .departed(departed)
                .daysFromNow(ChronoUnit.DAYS.between(now.toLocalDate(), departDate))
                .payTime(ticket.getPayTime())
                .createTime(ticket.getCreateTime())
                .build();
    }

    private String durationText(Integer minutes) {
        if (minutes == null) {
            return "";
        }
        return (minutes / 60) + "小时" + (minutes % 60) + "分";
    }

    private String statusText(Integer status) {
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

package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.service.SeatService;
import com.qiangpiao.vo.SeatMapVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 座位接口。
 */
@RestController
@RequestMapping("/api/seats")
@Api(tags = "座位")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{trainId}/{seatType}")
    @ApiOperation("座位图（1-商务座 2-一等座 3-二等座）")
    public R<SeatMapVO> seatMap(@PathVariable Long trainId, @PathVariable Integer seatType) {
        return R.ok(seatService.seatMap(trainId, seatType));
    }
}

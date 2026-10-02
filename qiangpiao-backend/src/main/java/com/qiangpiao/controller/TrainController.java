package com.qiangpiao.controller;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.R;
import com.qiangpiao.dataobject.TrainStopDO;
import com.qiangpiao.dto.RoutePlanQueryDTO;
import com.qiangpiao.dto.TrainQueryDTO;
import com.qiangpiao.service.RoutePlanService;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.RoutePlanVO;
import com.qiangpiao.vo.TrainDetailVO;
import com.qiangpiao.vo.TrainVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 车次接口（RESTful）。
 */
@RestController
@RequestMapping("/api/trains")
@Api(tags = "车次")
@Validated
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;
    private final RoutePlanService routePlanService;

    @GetMapping
    @ApiOperation("分页查询车次")
    public R<PageResult<TrainVO>> list(@Valid TrainQueryDTO queryDTO) {
        return R.ok(trainService.query(queryDTO));
    }

    @GetMapping("/{trainId}")
    @ApiOperation("车次详情（含座位图）；带 from/to 时余票与座位图按该乘车区间计算")
    public R<TrainDetailVO> detail(@PathVariable Long trainId,
                                   @RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to) {
        return R.ok(trainService.detail(trainId, from, to));
    }

    @GetMapping("/{trainId}/buy-block")
    @ApiOperation("购票资格预检：每人每天每车次 1 张 / 行程运行时间内不可重复购票")
    public R<java.util.Map<String, Object>> buyBlock(@PathVariable Long trainId) {
        String reason = trainService.buyBlockReason(com.qiangpiao.common.util.SecurityUtils.currentUserId(), trainId);
        java.util.Map<String, Object> data = new java.util.HashMap<>(2);
        data.put("canBuy", reason == null);
        data.put("reason", reason);
        return R.ok(data);
    }

    @GetMapping("/{trainId}/stops")
    @ApiOperation("车次时刻表（站点时序）：按停靠顺序返回到发时刻")
    public R<List<TrainStopDO>> stops(@PathVariable Long trainId) {
        return R.ok(trainService.stops(trainId));
    }

    @GetMapping("/stops/passing")
    @ApiOperation("经停时刻查询（车站大屏）：某天经停该站的全部车次及到发时刻")
    public R<List<TrainStopDO>> passing(@RequestParam String station,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(trainService.passingStops(station, date));
    }

    @GetMapping("/routes")
    @ApiOperation("中转方案推荐：直达 + 换乘一起算，支持最短耗时 / 最省钱 / 最少换乘")
    public R<List<RoutePlanVO>> routes(@Valid RoutePlanQueryDTO query) {
        return R.ok(routePlanService.plan(query));
    }
}

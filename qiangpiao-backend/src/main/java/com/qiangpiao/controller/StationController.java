package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.service.StationService;
import com.qiangpiao.vo.StationVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 车站接口。
 */
@RestController
@RequestMapping("/api/stations")
@Api(tags = "车站")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping
    @ApiOperation("全部车站（前端可本地缓存）")
    public R<List<StationVO>> list() {
        return R.ok(stationService.listAll());
    }

    @GetMapping("/search")
    @ApiOperation("车站模糊搜索")
    public R<List<StationVO>> search(@RequestParam(required = false) String keyword) {
        return R.ok(stationService.search(keyword));
    }
}

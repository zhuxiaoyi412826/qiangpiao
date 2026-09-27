package com.qiangpiao.controller;

import com.qiangpiao.common.result.PageResult;
import com.qiangpiao.common.result.R;
import com.qiangpiao.dto.TrainQueryDTO;
import com.qiangpiao.service.TrainService;
import com.qiangpiao.vo.TrainDetailVO;
import com.qiangpiao.vo.TrainVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

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

    @GetMapping
    @ApiOperation("分页查询车次")
    public R<PageResult<TrainVO>> list(@Valid TrainQueryDTO queryDTO) {
        return R.ok(trainService.query(queryDTO));
    }

    @GetMapping("/{trainId}")
    @ApiOperation("车次详情（含座位图）")
    public R<TrainDetailVO> detail(@PathVariable Long trainId) {
        return R.ok(trainService.detail(trainId));
    }
}

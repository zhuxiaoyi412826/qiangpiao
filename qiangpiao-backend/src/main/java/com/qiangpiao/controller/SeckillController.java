package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.IpUtils;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.dto.SeckillDTO;
import com.qiangpiao.service.SeckillService;
import com.qiangpiao.vo.SeckillResultVO;
import com.qiangpiao.vo.SeckillStatusVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 秒杀抢票接口。
 */
@RestController
@RequestMapping("/api/seckill")
@Api(tags = "秒杀抢票")
@Validated
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    @PostMapping("/do")
    @ApiOperation("抢票（异步下单，返回排队中）")
    public R<SeckillResultVO> seckill(@Valid @RequestBody SeckillDTO seckillDTO, HttpServletRequest request) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(seckillService.seckill(userId, seckillDTO, IpUtils.getIp(request)));
    }

    @GetMapping("/result")
    @ApiOperation("轮询抢票结果")
    public R<SeckillStatusVO> result(@RequestParam Long trainId, @RequestParam Integer seatType) {
        Long userId = SecurityUtils.currentUserId();
        return R.ok(seckillService.queryResult(trainId, seatType, userId));
    }

    @GetMapping("/stock")
    @ApiOperation("查询实时余票（Redis 库存）")
    public R<Integer> stock(@RequestParam Long trainId, @RequestParam Integer seatType) {
        return R.ok(seckillService.availableStock(trainId, seatType));
    }

    @PostMapping("/preheat/{trainId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiOperation("预热指定车次的秒杀库存（管理员）")
    public R<Void> preheat(@PathVariable Long trainId) {
        seckillService.preheatStock(trainId);
        return R.ok();
    }

    @PostMapping("/preheat")
    @PreAuthorize("hasRole('ADMIN')")
    @ApiOperation("全量预热秒杀库存（管理员）")
    public R<Void> preheatAll() {
        seckillService.preheatAllStock();
        return R.ok();
    }
}

package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.common.util.SecurityUtils;
import com.qiangpiao.dto.PassengerDTO;
import com.qiangpiao.service.PassengerService;
import com.qiangpiao.vo.PassengerVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 常用乘车人接口（最多 3 位）。
 * <p>
 * 身份证对外只返回脱敏值；下单时前端只回传乘车人 id，由服务端取明文。
 */
@RestController
@RequestMapping("/api/passengers")
@Api(tags = "常用乘车人")
@Validated
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    @GetMapping
    @ApiOperation("我的常用乘车人（身份证脱敏）")
    public R<List<PassengerVO>> list() {
        return R.ok(passengerService.list(SecurityUtils.currentUserId()));
    }

    @PostMapping
    @ApiOperation("新增常用乘车人（身份证明文传入，服务端加密落库，最多 3 位）")
    public R<Long> add(@Valid @RequestBody PassengerDTO dto) {
        return R.ok(passengerService.add(SecurityUtils.currentUserId(), dto));
    }

    @PutMapping("/{id}")
    @ApiOperation("修改常用乘车人")
    public R<Boolean> update(@PathVariable Long id, @Valid @RequestBody PassengerDTO dto) {
        passengerService.update(SecurityUtils.currentUserId(), id, dto);
        return R.ok(true);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除常用乘车人")
    public R<Boolean> delete(@PathVariable Long id) {
        passengerService.delete(SecurityUtils.currentUserId(), id);
        return R.ok(true);
    }
}

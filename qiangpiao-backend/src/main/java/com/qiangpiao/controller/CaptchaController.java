package com.qiangpiao.controller;

import com.qiangpiao.common.result.R;
import com.qiangpiao.dto.CaptchaVerifyDTO;
import com.qiangpiao.service.CaptchaService;
import com.qiangpiao.vo.CaptchaVO;
import com.qiangpiao.vo.SliderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 人机验证接口（抢票前置）。
 */
@RestController
@RequestMapping("/api/captcha")
@Api(tags = "人机验证")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;

    @GetMapping("/image")
    @ApiOperation("获取图形验证码（Base64 图片）")
    public R<CaptchaVO> image() {
        return R.ok(captchaService.generateImage());
    }

    @GetMapping("/slider")
    @ApiOperation("获取滑块验证码（背景图 + 拼图块）")
    public R<SliderVO> slider() {
        return R.ok(captchaService.generateSlider());
    }

    @PostMapping("/verify")
    @ApiOperation("校验人机验证结果（图形或滑块二选一，一次性）")
    public R<Boolean> verify(@RequestBody CaptchaVerifyDTO dto) {
        return R.ok(captchaService.verify(dto));
    }
}

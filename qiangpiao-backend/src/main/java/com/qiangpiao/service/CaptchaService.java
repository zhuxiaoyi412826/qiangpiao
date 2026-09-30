package com.qiangpiao.service;

import com.qiangpiao.dto.CaptchaVerifyDTO;
import com.qiangpiao.vo.CaptchaVO;
import com.qiangpiao.vo.SliderVO;

/**
 * 人机验证：图形验证码 / 滑块验证码。
 * <p>
 * 验证码文本与缺口坐标只存 Redis（一次性，5 分钟过期），不返回给前端。
 */
public interface CaptchaService {

    /** 生成图形验证码 */
    CaptchaVO generateImage();

    /** 生成滑块验证码（不返回缺口位置） */
    SliderVO generateSlider();

    /** 校验图形验证码（一次性，校验即失效） */
    boolean verifyImage(String captchaId, String code);

    /** 校验滑块位置（一次性，误差 6px 内通过） */
    boolean verifySlider(String sliderId, Integer x);

    /** 按入参自动选择校验方式：图形或滑块二选一 */
    boolean verify(CaptchaVerifyDTO dto);
}

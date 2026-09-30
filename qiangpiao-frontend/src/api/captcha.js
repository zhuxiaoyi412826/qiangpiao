import request from '@/utils/request'

/** 图形验证码：返回 { captchaId, image, expireSeconds } */
export function getCaptchaImage() {
    return request.get('/captcha/image')
}

/** 滑块验证码：返回 { sliderId, background, block, blockY, ... } */
export function getSlider() {
    return request.get('/captcha/slider')
}

/** 校验人机验证（图形或滑块二选一），一次性 */
export function verifyCaptcha(data) {
    return request.post('/captcha/verify', data)
}

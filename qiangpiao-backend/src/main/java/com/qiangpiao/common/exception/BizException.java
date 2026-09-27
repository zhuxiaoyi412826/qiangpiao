package com.qiangpiao.common.exception;

import com.qiangpiao.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：预期内的业务规则不满足，由全局异常处理器转换为 ResultCode 响应。
 */
@Getter
public class BizException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int code;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        super(message);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
    }
}

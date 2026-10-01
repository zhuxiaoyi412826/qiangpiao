package com.qiangpiao.common.exception;

import com.qiangpiao.common.result.R;
import com.qiangpiao.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * common 包全局异常处理器：统一把异常转换为 REST 出参。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常
     */
    @ExceptionHandler(BizException.class)
    public R<Void> handleBizException(BizException e) {
        log.warn("业务异常 -> code={}, msg={}", e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    /**
     * @RequestBody + @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors();
        String msg = errors.isEmpty() ? ResultCode.BAD_REQUEST.getMessage()
                : errors.stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败 -> {}", msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 表单绑定 / @Validated 校验失败
     */
    @ExceptionHandler(BindException.class)
    public R<Void> handleBindException(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败 -> {}", msg);
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 方法级参数校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 缺少必传参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "缺少参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "参数[" + e.getName() + "]类型不正确");
    }

    /**
     * JSON 解析失败
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "请求体格式错误，无法解析");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public R<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return R.fail(ResultCode.METHOD_NOT_ALLOWED.getCode(), "不支持的请求方式：" + e.getMethod());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public R<Void> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "不支持的媒体类型：" + e.getContentType());
    }

    /**
     * 404
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNotFound(NoHandlerFoundException e) {
        return R.fail(ResultCode.NOT_FOUND.getCode(), "接口不存在：" + e.getRequestURL());
    }

    /**
     * 唯一键冲突：并发重复下单 / 重复添加乘车人等
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public R<Void> handleDuplicateKey(DuplicateKeyException e) {
        log.warn("唯一键冲突 -> {}", e.getRootCause() == null ? e.getMessage() : e.getRootCause().getMessage());
        return R.fail(ResultCode.BAD_REQUEST.getCode(), "数据已存在，请勿重复提交");
    }

    /**
     * 数据写入违反 DB 约束（字段超长 / 非空 / 外键）。
     * 归到"系统异常"里极难定位（典型：敏感字段改密文存储后列长不够），
     * 这里单独识别并给出可执行的提示。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleDataIntegrity(DataIntegrityViolationException e) {
        Throwable root = e.getRootCause() != null ? e.getRootCause() : e;
        String rootMsg = root.getMessage() == null ? "" : root.getMessage();
        if (rootMsg.contains("Data too long")) {
            // 最常见根因：列按明文长度设计，改存 AES 密文后放不下（手机号 11 → 28，身份证 18 → 48）
            log.error("字段超长，请检查该列长度是否容得下密文：{}", rootMsg);
            return R.fail(ResultCode.SYSTEM_ERROR.getCode(), "数据写入失败：字段长度不足，请联系管理员");
        }
        log.error("数据约束冲突", e);
        return R.fail(ResultCode.SYSTEM_ERROR.getCode(), "数据写入失败：违反数据库约束");
    }

    /**
     * 认证失败
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuthentication(AuthenticationException e) {
        return R.fail(ResultCode.UNAUTHORIZED.getCode(), e.getMessage());
    }

    /**
     * 鉴权失败
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN.getCode(), "没有访问权限");
    }

    /**
     * 兜底：未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return R.fail(ResultCode.SYSTEM_ERROR.getCode(), ResultCode.SYSTEM_ERROR.getMessage());
    }
}

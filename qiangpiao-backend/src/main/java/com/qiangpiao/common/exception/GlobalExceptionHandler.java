package com.qiangpiao.common.exception;

import com.qiangpiao.common.result.R;
import com.qiangpiao.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
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

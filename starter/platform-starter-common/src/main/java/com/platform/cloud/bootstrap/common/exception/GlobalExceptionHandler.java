package com.platform.cloud.bootstrap.common.exception;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。把所有异常转成统一的 R&lt;T&gt; 响应。
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusiness(BusinessException e, HttpServletRequest req) {
        log.warn("BusinessException at {} {}: code={}, msg={}", req.getMethod(), req.getRequestURI(), e.getCode(), e.getMessage());
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_INVALID, detail);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBind(BindException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_INVALID, detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(this::formatViolation)
                .collect(Collectors.joining("; "));
        return R.fail(ResultCode.PARAM_INVALID, detail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return R.fail(ResultCode.PARAM_MISSING, "缺少参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return R.fail(ResultCode.PARAM_INVALID, "请求体格式错误");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public R<Void> handleMethod(HttpRequestMethodNotSupportedException e) {
        return R.fail(ResultCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<Void> handleNoResource(NoResourceFoundException e) {
        return R.fail(ResultCode.NOT_FOUND, "资源不存在: " + e.getResourcePath());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleNoAuth(AuthenticationCredentialsNotFoundException e) {
        return R.fail(ResultCode.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleBadCredentials(BadCredentialsException e) {
        return R.fail(ResultCode.UNAUTHORIZED, "用户名或密码错误");
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public R<Void> handleAuth(AuthenticationException e) {
        return R.fail(ResultCode.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<Void> handleAccessDenied(AccessDeniedException e) {
        return R.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleAny(Throwable e, HttpServletRequest req) {
        log.error("Unhandled exception at {} {}", req.getMethod(), req.getRequestURI(), e);
        return R.fail(ResultCode.INTERNAL_ERROR, e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    private String formatFieldError(FieldError fe) {
        return fe.getField() + " " + fe.getDefaultMessage();
    }

    private String formatViolation(ConstraintViolation<?> v) {
        return v.getPropertyPath() + " " + v.getMessage();
    }
}

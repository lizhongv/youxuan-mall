package com.platform.cloud.bootstrap.common.exception;

import com.platform.cloud.bootstrap.common.web.ResultCode;
import lombok.Getter;

import java.io.Serial;

/**
 * 受检的业务异常。GlobalExceptionHandler 会把它转成对应的 HTTP 响应。
 */
@Getter
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final int code;

    public BusinessException(ResultCode rc) {
        super(rc.getMsg());
        this.code = rc.getCode();
    }

    public BusinessException(ResultCode rc, String detail) {
        super(detail);
        this.code = rc.getCode();
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BusinessException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
    }
}

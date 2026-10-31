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

    /**
     * 附带给前端的结构化数据（可空），例如登录失败时的 {needCaptcha: true}。
     * transient：异常本身不需要跨进程序列化，数据只用于本次响应。
     */
    private final transient Object data;

    /** 最常用：业务规则不满足，直接给用户看的提示，如「库存不足」 */
    public BusinessException(String msg) {
        super(msg);
        this.code = ResultCode.BUSINESS_ERROR.getCode();
        this.data = null;
    }

    public BusinessException(ResultCode rc) {
        super(rc.getMsg());
        this.code = rc.getCode();
        this.data = null;
    }

    public BusinessException(ResultCode rc, String detail) {
        super(detail);
        this.code = rc.getCode();
        this.data = null;
    }

    public BusinessException(ResultCode rc, String detail, Object data) {
        super(detail);
        this.code = rc.getCode();
        this.data = data;
    }

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
        this.data = null;
    }

    public BusinessException(int code, String msg, Throwable cause) {
        super(msg, cause);
        this.code = code;
        this.data = null;
    }
}

package com.platform.cloud.bootstrap.common.web;

import lombok.Getter;

/**
 * 业务错误码枚举。
 * 5 位数字：前 3 位映射 HTTP 语义，后 2 位为子类细分。
 * 0 留给成功；4xxxx 客户端错误；5xxxx 服务端错误；6xxxx 业务错误。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),

    PARAM_INVALID(40001, "参数错误"),
    PARAM_MISSING(40002, "缺少必要参数"),
    UNAUTHORIZED(40100, "未登录或登录已过期"),
    FORBIDDEN(40300, "无权访问"),
    NOT_FOUND(40400, "资源不存在"),
    METHOD_NOT_ALLOWED(40500, "请求方法不允许"),
    DUPLICATE_REQUEST(42900, "请求过于频繁，请稍后再试"),

    INTERNAL_ERROR(50000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(50300, "服务暂不可用"),

    BUSINESS_ERROR(60000, "业务异常");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

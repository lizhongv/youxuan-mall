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
    /** 管理员仍在使用初始密码或被他人重置的密码：除查看个人信息、改密码、登出外的管理端接口一律拒绝 */
    PASSWORD_CHANGE_REQUIRED(40301, "请先修改初始密码"),
    NOT_FOUND(40400, "资源不存在"),
    METHOD_NOT_ALLOWED(40500, "请求方法不允许"),
    DUPLICATE_REQUEST(42900, "请求过于频繁，请稍后再试"),

    INTERNAL_ERROR(50000, "服务器内部错误"),
    SERVICE_UNAVAILABLE(50300, "服务暂不可用"),

    BUSINESS_ERROR(60000, "业务异常"),

    /*
     * 余额支付密码。单独编码而不是统一 60000：前端要据此分别引导「去设置支付密码」、
     * 「提示剩余次数后重新输入」、「锁定倒计时」，只靠 msg 文案判断很脆弱。
     */
    /** 余额支付时会员尚未设置支付密码 */
    PAY_PASSWORD_NOT_SET(60010, "请先设置支付密码"),
    /** 支付密码错误；msg 由调用方带上剩余次数 */
    PAY_PASSWORD_WRONG(60011, "支付密码错误"),
    /** 连续错误次数达到上限，已锁定 */
    PAY_PASSWORD_LOCKED(60012, "支付密码错误次数过多，请 30 分钟后再试");

    private final int code;
    private final String msg;

    ResultCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

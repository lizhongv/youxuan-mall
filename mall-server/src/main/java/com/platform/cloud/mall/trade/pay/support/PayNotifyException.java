package com.platform.cloud.mall.trade.pay.support;

/**
 * 渠道回调不可信或与本地数据不符（验签失败、商户号 / AppID 不符、金额不符）。
 * <p>
 * 回调接口捕获后按渠道要求返回失败；金额不符这类情况渠道会重试，但每次都会失败并留下错误日志，便于人工介入。
 */
public class PayNotifyException extends RuntimeException {

    public PayNotifyException(String message) {
        super(message);
    }

    public PayNotifyException(String message, Throwable cause) {
        super(message, cause);
    }
}

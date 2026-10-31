package com.platform.cloud.mall.trade.pay.channel.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝异步通知验签（官方 SDK AlipaySignature.rsaCheckV1，RSA2）。
 * <p>
 * 单独成组件是因为 SDK 提供的是静态方法，抽出来单测才能替换。
 */
@Component
public class AlipaySignatureVerifier {

    static final String CHARSET = "UTF-8";
    static final String SIGN_TYPE = "RSA2";

    /** 验签通过返回 true；SDK 会修改入参 Map（移除 sign / sign_type），这里传副本 */
    public boolean verify(Map<String, String> params, String alipayPublicKey) {
        try {
            return AlipaySignature.rsaCheckV1(new HashMap<>(params), alipayPublicKey, CHARSET, SIGN_TYPE);
        } catch (AlipayApiException e) {
            return false;
        }
    }
}

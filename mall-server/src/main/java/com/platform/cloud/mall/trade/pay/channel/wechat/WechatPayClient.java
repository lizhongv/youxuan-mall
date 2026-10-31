package com.platform.cloud.mall.trade.pay.channel.wechat;

import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;

/**
 * 微信支付 APIv3 官方 SDK（wechatpay-java）的薄封装：SDK 的各 Service 是具体类且构建时要联网，
 * 抽一层接口便于按配置重建，也便于单测 mock。入参出参直接使用 SDK 的模型类。
 * <p>
 * 失败时抛 SDK 异常：ServiceException（微信返回错误码）、HttpException（网络）、ValidationException（验签失败）。
 */
public interface WechatPayClient {

    /** JSAPI 下单并按 prepay_id 生成前端调起签名（SDK 内完成 paySign 计算） */
    PrepayWithRequestPaymentResponse jsapiPrepay(com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest req);

    /** H5 下单，返回 h5_url（5 分钟有效） */
    String h5Prepay(com.wechat.pay.java.service.payments.h5.model.PrepayRequest req);

    Transaction queryByOutTradeNo(String mchId, String outTradeNo);

    void closeOrder(String mchId, String outTradeNo);

    Refund createRefund(CreateRequest req);

    Refund queryRefund(String outRefundNo);

    /** 回调验签 + 解密；签名不对抛 ValidationException */
    <T> T parseNotification(RequestParam param, Class<T> clazz);
}

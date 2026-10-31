package com.platform.cloud.mall.trade.pay.controller.app;

import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.channel.alipay.AlipayChannel;
import com.platform.cloud.mall.trade.pay.channel.wechat.WechatPayChannel;
import com.platform.cloud.mall.trade.pay.channel.wechat.WechatPayChannel.NotifyHeaders;
import com.platform.cloud.mall.trade.pay.channel.wechat.WechatPayChannel.RefundNotice;
import com.platform.cloud.mall.trade.pay.service.PaySettlementService;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付渠道异步通知（公开路径 /app/public/**，无需登录）。
 * <p>
 * 只信任验签结果：任何字段都以 SDK 验签 / 解密后的内容为准，再交给 {@link PaySettlementService#settle}
 * （金额以分核对、幂等）。响应格式按渠道要求，不使用统一响应包装：
 * <ul>
 *   <li>微信：成功 HTTP 204 无应答体；失败 4XX / 5XX + {@code {"code":"FAIL","message":"..."}}，微信会按策略重试；</li>
 *   <li>支付宝：成功返回纯文本 {@code success}，其他内容视为失败并重试。</li>
 * </ul>
 */
@Slf4j
@Tag(name = "支付回调")
@RestController
@RequestMapping("/app/public/pay/notify")
@RequiredArgsConstructor
public class PayNotifyController {

    static final String ALIPAY_SUCCESS = "success";
    static final String ALIPAY_FAIL = "fail";

    private final WechatPayChannel wechatPayChannel;
    private final AlipayChannel alipayChannel;
    private final PaySettlementService settlement;

    @Operation(summary = "微信支付结果通知")
    @PostMapping("/wechat")
    public ResponseEntity<String> wechat(
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Signature-Type", required = false) String signType,
            @RequestBody(required = false) byte[] body) {
        try {
            // 验签基于原始报文字节，按 UTF-8 还原后交给 SDK，不经过 JSON 反序列化再序列化
            ChannelTrade t = wechatPayChannel.parsePayNotify(
                    new NotifyHeaders(serial, nonce, signature, timestamp, signType), text(body));
            settlement.settle(t);
            return ResponseEntity.noContent().build();
        } catch (PayNotifyException e) {
            log.warn("微信支付通知被拒绝：{}", e.getMessage());
            return wechatFail(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("微信支付通知处理失败，等待微信重试", e);
            return wechatFail(HttpStatus.INTERNAL_SERVER_ERROR, "处理失败");
        }
    }

    @Operation(summary = "微信退款结果通知")
    @PostMapping("/wechat-refund")
    public ResponseEntity<String> wechatRefund(
            @RequestHeader(value = "Wechatpay-Serial", required = false) String serial,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Signature-Type", required = false) String signType,
            @RequestBody(required = false) byte[] body) {
        try {
            RefundNotice n = wechatPayChannel.parseRefundNotify(
                    new NotifyHeaders(serial, nonce, signature, timestamp, signType), text(body));
            settlement.onRefundResult(n.refundNo(), n.result());
            return ResponseEntity.noContent().build();
        } catch (PayNotifyException e) {
            log.warn("微信退款通知被拒绝：{}", e.getMessage());
            return wechatFail(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            log.error("微信退款通知处理失败，等待微信重试", e);
            return wechatFail(HttpStatus.INTERNAL_SERVER_ERROR, "处理失败");
        }
    }

    @Operation(summary = "支付宝异步通知（application/x-www-form-urlencoded）")
    @PostMapping(value = "/alipay", produces = MediaType.TEXT_PLAIN_VALUE)
    public String alipay(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((k, v) -> params.put(k, v == null || v.length == 0 ? "" : v[0]));
        try {
            settlement.settle(alipayChannel.parseNotify(params));
            return ALIPAY_SUCCESS;
        } catch (PayNotifyException e) {
            log.warn("支付宝通知被拒绝：{}", e.getMessage());
            return ALIPAY_FAIL;
        } catch (RuntimeException e) {
            log.error("支付宝通知处理失败，等待支付宝重试 outTradeNo={}", params.get("out_trade_no"), e);
            return ALIPAY_FAIL;
        }
    }

    private static String text(byte[] body) {
        return body == null ? "" : new String(body, StandardCharsets.UTF_8);
    }

    private static ResponseEntity<String> wechatFail(HttpStatus status, String message) {
        String safe = message == null ? "失败" : message.replace("\"", "'");
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"FAIL\",\"message\":\"" + safe + "\"}");
    }
}

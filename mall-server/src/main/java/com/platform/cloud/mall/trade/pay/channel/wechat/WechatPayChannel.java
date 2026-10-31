package com.platform.cloud.mall.trade.pay.channel.wechat;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.mapper.PaymentMapper;
import com.platform.cloud.mall.trade.pay.support.ConfiguredClientCache;
import com.platform.cloud.mall.trade.pay.support.PayAmounts;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.exception.WechatPayException;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.h5.model.H5Info;
import com.wechat.pay.java.service.payments.h5.model.SceneInfo;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import com.wechat.pay.java.service.refund.model.RefundNotification;
import com.wechat.pay.java.service.refund.model.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * 微信支付 APIv3：公众号 JSAPI 与 H5（MWEB）两种发起方式共用商户号、回调与退款。
 * <p>
 * 配置存 sys_config，每次使用时取最新配置；配置变化时 {@link ConfiguredClientCache} 重建 SDK 客户端。
 * 日志中只出现商户号、单号、金额，不出现私钥与 APIv3 密钥。
 */
@Slf4j
@Component
public class WechatPayChannel implements AsyncPayChannel {

    public static final String TYPE = PayType.CHANNEL_WECHAT;
    static final String CURRENCY = "CNY";
    /** 微信 description 最长 127 字节（UTF-8），按字节截断 */
    static final int DESCRIPTION_MAX_BYTES = 127;
    /** 商户订单号不存在（尚未下单或已过期清理） */
    static final String ORDER_NOT_EXIST = "ORDER_NOT_EXIST";
    static final DateTimeFormatter RFC3339 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final PaySettings settings;
    private final PaymentMapper paymentMapper;
    private final ConfiguredClientCache<PaySettings.WechatPay, WechatPayClient> clients;

    @Autowired
    public WechatPayChannel(PaySettings settings, PaymentMapper paymentMapper) {
        this(settings, paymentMapper, SdkWechatPayClient::create);
    }

    /** 单测注入 mock 客户端 */
    WechatPayChannel(PaySettings settings, PaymentMapper paymentMapper,
                     Function<PaySettings.WechatPay, WechatPayClient> builder) {
        this.settings = settings;
        this.paymentMapper = paymentMapper;
        this.clients = new ConfiguredClientCache<>("微信支付", builder);
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Availability availability(PayType payType) {
        PaySettings.WechatPay s = settings.wechat();
        boolean on = payType == PayType.WECHAT_JSAPI ? s.jsapiEnabled() : payType == PayType.WECHAT_H5 && s.h5Enabled();
        if (!on) {
            return Availability.off();
        }
        if (s.missing() != null || settings.notifyUrl(PaySettings.NOTIFY_WECHAT) == null) {
            return Availability.notReady("微信支付暂未配置完成，请选择其他支付方式");
        }
        if (clients.get(s).isEmpty()) {
            return Availability.notReady("微信支付暂不可用，请选择其他支付方式");
        }
        return Availability.available();
    }

    @Override
    public Map<String, String> prepay(PrepayCommand cmd) {
        PaySettings.WechatPay s = settings.wechat();
        WechatPayClient client = require(s);
        String notifyUrl = settings.notifyUrl(PaySettings.NOTIFY_WECHAT);
        String description = truncateBytes(cmd.subject(), DESCRIPTION_MAX_BYTES);
        String timeExpire = rfc3339(cmd.expireAt());
        int total = Math.toIntExact(PayAmounts.toFen(cmd.amount()));
        try {
            if (cmd.payType() == PayType.WECHAT_JSAPI) {
                PrepayRequest req = new PrepayRequest();
                req.setAppid(s.appId());
                req.setMchid(s.mchId());
                req.setDescription(description);
                req.setOutTradeNo(cmd.payNo());
                req.setTimeExpire(timeExpire);
                req.setNotifyUrl(notifyUrl);
                Amount amount = new Amount();
                amount.setTotal(total);
                amount.setCurrency(CURRENCY);
                req.setAmount(amount);
                Payer payer = new Payer();
                payer.setOpenid(cmd.openid());
                req.setPayer(payer);
                PrepayWithRequestPaymentResponse r = client.jsapiPrepay(req);
                // 键名与 WeixinJSBridge getBrandWCPayRequest / wx.chooseWXPay 参数一致，前端原样传入
                Map<String, String> params = new LinkedHashMap<>();
                params.put("appId", r.getAppId());
                params.put("timeStamp", r.getTimeStamp());
                params.put("nonceStr", r.getNonceStr());
                params.put("package", r.getPackageVal());
                params.put("signType", r.getSignType());
                params.put("paySign", r.getPaySign());
                return params;
            }
            com.wechat.pay.java.service.payments.h5.model.PrepayRequest req =
                    new com.wechat.pay.java.service.payments.h5.model.PrepayRequest();
            req.setAppid(s.appId());
            req.setMchid(s.mchId());
            req.setDescription(description);
            req.setOutTradeNo(cmd.payNo());
            req.setTimeExpire(timeExpire);
            req.setNotifyUrl(notifyUrl);
            com.wechat.pay.java.service.payments.h5.model.Amount amount =
                    new com.wechat.pay.java.service.payments.h5.model.Amount();
            amount.setTotal(total);
            amount.setCurrency(CURRENCY);
            req.setAmount(amount);
            SceneInfo scene = new SceneInfo();
            // H5 支付必须上送用户真实 IP，微信会校验与实际发起支付的终端是否一致
            scene.setPayerClientIp(cmd.clientIp());
            H5Info h5 = new H5Info();
            h5.setType("Wap");
            scene.setH5Info(h5);
            req.setSceneInfo(scene);
            return Map.of("h5Url", client.h5Prepay(req));
        } catch (ServiceException e) {
            log.warn("微信支付下单失败 payNo={} code={} msg={}", cmd.payNo(), e.getErrorCode(), e.getErrorMessage());
            throw new BusinessException("微信支付下单失败：" + e.getErrorMessage());
        } catch (WechatPayException e) {
            log.warn("微信支付下单异常 payNo={} {}", cmd.payNo(), e.getClass().getSimpleName());
            throw new BusinessException("微信支付暂时不可用，请稍后重试");
        }
    }

    @Override
    public ChannelTrade query(Payment payment) {
        PaySettings.WechatPay s = settings.wechat();
        try {
            return toTrade(require(s).queryByOutTradeNo(s.mchId(), payment.getPayNo()), payment.getPayNo());
        } catch (ServiceException e) {
            if (ORDER_NOT_EXIST.equals(e.getErrorCode())) {
                return new ChannelTrade(payment.getPayNo(), ChannelTrade.State.NOT_EXIST, "", 0);
            }
            throw new BusinessException("微信查单失败：" + e.getErrorMessage());
        } catch (WechatPayException e) {
            throw new BusinessException("微信查单失败，请稍后重试");
        }
    }

    @Override
    public void close(Payment payment) {
        PaySettings.WechatPay s = settings.wechat();
        try {
            require(s).closeOrder(s.mchId(), payment.getPayNo());
        } catch (ServiceException e) {
            // ORDERPAID 等错误：调用方据此知道不能关闭，需要查单处理
            throw new BusinessException("微信关单失败：" + e.getErrorMessage());
        } catch (WechatPayException e) {
            throw new BusinessException("微信关单失败，请稍后重试");
        }
    }

    @Override
    public RefundResult refund(RefundCommand cmd) {
        Payment payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery()
                .eq(Payment::getPayNo, cmd.payNo()));
        if (payment == null) {
            throw new BusinessException("支付单不存在：" + cmd.payNo());
        }
        CreateRequest req = new CreateRequest();
        req.setOutTradeNo(cmd.payNo());
        // 售后单号作为 out_refund_no：同一售后单重复提交时微信按单号幂等，不会退两次
        req.setOutRefundNo(cmd.refundNo());
        req.setReason("订单 " + cmd.orderNo() + " 退款");
        req.setNotifyUrl(settings.notifyUrl(PaySettings.NOTIFY_WECHAT_REFUND));
        AmountReq amount = new AmountReq();
        amount.setRefund(PayAmounts.toFen(cmd.amount()));
        amount.setTotal(PayAmounts.toFen(payment.getAmount()));
        amount.setCurrency(CURRENCY);
        req.setAmount(amount);
        try {
            Refund r = require(settings.wechat()).createRefund(req);
            log.info("微信退款已提交 payNo={} refundNo={} amount={} status={}", cmd.payNo(), cmd.refundNo(),
                    cmd.amount(), r.getStatus());
            return toRefundResult(r.getStatus());
        } catch (ServiceException e) {
            log.warn("微信退款失败 payNo={} refundNo={} code={} msg={}", cmd.payNo(), cmd.refundNo(),
                    e.getErrorCode(), e.getErrorMessage());
            throw new BusinessException("微信退款失败：" + e.getErrorMessage());
        } catch (WechatPayException e) {
            throw new BusinessException("微信退款请求失败，请稍后重试");
        }
    }

    @Override
    public RefundResult queryRefund(String payNo, String refundNo) {
        try {
            return toRefundResult(require(settings.wechat()).queryRefund(refundNo).getStatus());
        } catch (ServiceException e) {
            throw new BusinessException("微信退款查询失败：" + e.getErrorMessage());
        } catch (WechatPayException e) {
            throw new BusinessException("微信退款查询失败，请稍后重试");
        }
    }

    /**
     * 支付成功回调：SDK 用平台证书 / 微信支付公钥验签并用 APIv3 密钥解密，再核对商户号与 AppID。
     * 只信任解密后的内容，不信任回调里任何明文字段。
     */
    public ChannelTrade parsePayNotify(NotifyHeaders headers, String body) {
        PaySettings.WechatPay s = settings.wechat();
        Transaction t = parse(s, headers, body, Transaction.class);
        if (!s.mchId().equals(t.getMchid()) || !s.appId().equals(t.getAppid())) {
            throw new PayNotifyException("微信回调商户号或 AppID 与配置不符 outTradeNo=" + t.getOutTradeNo());
        }
        return toTrade(t, t.getOutTradeNo());
    }

    /** 退款结果回调 */
    public RefundNotice parseRefundNotify(NotifyHeaders headers, String body) {
        PaySettings.WechatPay s = settings.wechat();
        RefundNotification n = parse(s, headers, body, RefundNotification.class);
        return new RefundNotice(n.getOutTradeNo(), n.getOutRefundNo(), toRefundResult(n.getRefundStatus()));
    }

    private <T> T parse(PaySettings.WechatPay s, NotifyHeaders h, String body, Class<T> clazz) {
        WechatPayClient client = clients.get(s).orElseThrow(() -> new PayNotifyException("微信支付未配置"));
        RequestParam.Builder b = new RequestParam.Builder()
                .serialNumber(h.serial())
                .nonce(h.nonce())
                .signature(h.signature())
                .timestamp(h.timestamp())
                .body(body);
        if (h.signType() != null && !h.signType().isBlank()) {
            // 不传时 SDK 默认 WECHATPAY2-SHA256-RSA2048
            b.signType(h.signType());
        }
        RequestParam param = b.build();
        try {
            return client.parseNotification(param, clazz);
        } catch (ValidationException e) {
            throw new PayNotifyException("微信回调验签失败", e);
        } catch (RuntimeException e) {
            // 报文格式错误、解密失败（APIv3 密钥不对）等，同样视为不可信
            throw new PayNotifyException("微信回调解析失败：" + e.getClass().getSimpleName(), e);
        }
    }

    private WechatPayClient require(PaySettings.WechatPay s) {
        if (s.missing() != null) {
            throw new BusinessException("微信支付未配置：" + s.missing());
        }
        Optional<WechatPayClient> c = clients.get(s);
        return c.orElseThrow(() -> new BusinessException("微信支付暂不可用，请检查商户配置"));
    }

    static ChannelTrade toTrade(Transaction t, String payNo) {
        ChannelTrade.State state = switch (t.getTradeState() == null ? Transaction.TradeStateEnum.NOTPAY
                : t.getTradeState()) {
            case SUCCESS -> ChannelTrade.State.PAID;
            case NOTPAY, USERPAYING, ACCEPT -> ChannelTrade.State.WAITING;
            case REFUND -> ChannelTrade.State.REFUNDED;
            case CLOSED, REVOKED, PAYERROR -> ChannelTrade.State.CLOSED;
        };
        long total = t.getAmount() == null || t.getAmount().getTotal() == null ? -1 : t.getAmount().getTotal();
        return new ChannelTrade(payNo, state, t.getTransactionId() == null ? "" : t.getTransactionId(), total);
    }

    static RefundResult toRefundResult(Status status) {
        if (status == null) {
            return RefundResult.processing();
        }
        return switch (status) {
            case SUCCESS -> RefundResult.success();
            case PROCESSING -> RefundResult.processing();
            case CLOSED -> RefundResult.failed("微信退款已关闭");
            case ABNORMAL -> RefundResult.failed("微信退款异常（用户银行卡可能已注销），请到商户平台发起异常退款");
        };
    }

    /** 微信要求 RFC3339，如 2026-10-31T12:00:00+08:00，不带毫秒 */
    static String rfc3339(LocalDateTime t) {
        return t == null ? null : t.truncatedTo(ChronoUnit.SECONDS).atZone(ZONE).toOffsetDateTime().format(RFC3339);
    }

    static String truncateBytes(String s, int maxBytes) {
        if (s == null) {
            return "";
        }
        if (s.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        int bytes = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            int len = new String(Character.toChars(cp)).getBytes(StandardCharsets.UTF_8).length;
            if (bytes + len > maxBytes) {
                break;
            }
            sb.appendCodePoint(cp);
            bytes += len;
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    /** 回调请求头（Wechatpay-*） */
    public record NotifyHeaders(String serial, String nonce, String signature, String timestamp, String signType) {
    }

    /** 退款回调内容 */
    public record RefundNotice(String payNo, String refundNo, RefundResult result) {
    }
}

package com.platform.cloud.mall.trade.pay.channel.alipay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeCloseModel;
import com.alipay.api.domain.AlipayTradeFastpayRefundQueryModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.alipay.api.response.AlipayTradeCloseResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.alipay.api.response.AlipayTradeWapPayResponse;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.channel.AsyncPayChannel;
import com.platform.cloud.mall.trade.pay.channel.ChannelTrade;
import com.platform.cloud.mall.trade.pay.config.PaySettings;
import com.platform.cloud.mall.trade.pay.entity.Payment;
import com.platform.cloud.mall.trade.pay.support.ConfiguredClientCache;
import com.platform.cloud.mall.trade.pay.support.PayAmounts;
import com.platform.cloud.mall.trade.pay.support.PayNotifyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 支付宝手机网站支付（alipay.trade.wap.pay），官方 SDK alipay-sdk-java，公钥模式、RSA2 签名。
 * <p>
 * 下单用 pageExecute(GET) 生成带签名的跳转地址 payUrl，前端 location.href 即可，不必渲染表单；
 * 退款 alipay.trade.refund 是同步接口，fund_change = Y 即已退款，否则再查一次退款结果。
 */
@Slf4j
@Component
public class AlipayChannel implements AsyncPayChannel {

    public static final String TYPE = PayType.CHANNEL_ALIPAY;
    static final String PRODUCT_CODE = "QUICK_WAP_WAY";
    static final String TRADE_NOT_EXIST = "ACQ.TRADE_NOT_EXIST";
    static final String REFUND_SUCCESS = "REFUND_SUCCESS";
    /** subject 最长 256 字符，取 128 足够展示 */
    static final int SUBJECT_MAX = 128;
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaySettings settings;
    private final AlipaySignatureVerifier verifier;
    private final ConfiguredClientCache<PaySettings.Alipay, AlipayClient> clients;

    @Autowired
    public AlipayChannel(PaySettings settings, AlipaySignatureVerifier verifier) {
        this(settings, verifier, AlipayChannel::createClient);
    }

    /** 单测注入 mock 的 AlipayClient */
    AlipayChannel(PaySettings settings, AlipaySignatureVerifier verifier,
                  Function<PaySettings.Alipay, AlipayClient> builder) {
        this.settings = settings;
        this.verifier = verifier;
        this.clients = new ConfiguredClientCache<>("支付宝", builder);
    }

    static AlipayClient createClient(PaySettings.Alipay s) {
        AlipayConfig c = new AlipayConfig();
        c.setServerUrl(s.gateway());
        c.setAppId(s.appId());
        c.setPrivateKey(s.privateKey());
        c.setAlipayPublicKey(s.alipayPublicKey());
        c.setFormat("json");
        c.setCharset(AlipaySignatureVerifier.CHARSET);
        c.setSignType(AlipaySignatureVerifier.SIGN_TYPE);
        try {
            return new DefaultAlipayClient(c);
        } catch (AlipayApiException e) {
            throw new IllegalStateException("支付宝客户端初始化失败：" + e.getErrMsg());
        }
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public Availability availability(PayType payType) {
        PaySettings.Alipay s = settings.alipay();
        if (payType != PayType.ALIPAY_WAP || !s.enabled()) {
            return Availability.off();
        }
        if (s.missing() != null || settings.notifyUrl(PaySettings.NOTIFY_ALIPAY) == null) {
            return Availability.notReady("支付宝暂未配置完成，请选择其他支付方式");
        }
        if (clients.get(s).isEmpty()) {
            return Availability.notReady("支付宝暂不可用，请选择其他支付方式");
        }
        return Availability.available();
    }

    @Override
    public Map<String, String> prepay(PrepayCommand cmd) {
        AlipayClient client = require();
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(cmd.payNo());
        model.setTotalAmount(PayAmounts.toYuanText(cmd.amount()));
        model.setSubject(cmd.subject().length() <= SUBJECT_MAX ? cmd.subject() : cmd.subject().substring(0, SUBJECT_MAX));
        model.setProductCode(PRODUCT_CODE);
        if (cmd.expireAt() != null) {
            // 绝对超时时间：与订单支付截止时间一致，过期后支付宝拒绝付款
            model.setTimeExpire(cmd.expireAt().format(TIME));
        }
        if (cmd.returnUrl() != null) {
            model.setQuitUrl(cmd.returnUrl());
        }
        AlipayTradeWapPayRequest req = new AlipayTradeWapPayRequest();
        req.setBizModel(model);
        req.setNotifyUrl(settings.notifyUrl(PaySettings.NOTIFY_ALIPAY));
        if (cmd.returnUrl() != null) {
            req.setReturnUrl(cmd.returnUrl());
        }
        try {
            AlipayTradeWapPayResponse r = client.pageExecute(req, "GET");
            if (!r.isSuccess() || !StringUtils.hasText(r.getBody())) {
                throw new BusinessException("支付宝下单失败：" + r.getSubMsg());
            }
            return Map.of("payUrl", r.getBody());
        } catch (AlipayApiException e) {
            log.warn("支付宝下单异常 payNo={} {}", cmd.payNo(), e.getErrMsg());
            throw new BusinessException("支付宝暂时不可用，请稍后重试");
        }
    }

    @Override
    public ChannelTrade query(Payment payment) {
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(payment.getPayNo());
        AlipayTradeQueryRequest req = new AlipayTradeQueryRequest();
        req.setBizModel(model);
        AlipayTradeQueryResponse r = execute(req, "支付宝查单");
        if (!r.isSuccess()) {
            if (TRADE_NOT_EXIST.equals(r.getSubCode())) {
                // 用户还没打开支付宝收银台时交易尚未创建
                return new ChannelTrade(payment.getPayNo(), ChannelTrade.State.NOT_EXIST, "", 0);
            }
            throw new BusinessException("支付宝查单失败：" + r.getSubMsg());
        }
        return new ChannelTrade(payment.getPayNo(), toState(r.getTradeStatus()),
                r.getTradeNo() == null ? "" : r.getTradeNo(), PayAmounts.yuanTextToFen(r.getTotalAmount()));
    }

    @Override
    public void close(Payment payment) {
        AlipayTradeCloseModel model = new AlipayTradeCloseModel();
        model.setOutTradeNo(payment.getPayNo());
        AlipayTradeCloseRequest req = new AlipayTradeCloseRequest();
        req.setBizModel(model);
        AlipayTradeCloseResponse r = execute(req, "支付宝关单");
        // 交易不存在说明用户从未打开收银台，等同已关闭
        if (!r.isSuccess() && !TRADE_NOT_EXIST.equals(r.getSubCode())) {
            throw new BusinessException("支付宝关单失败：" + r.getSubMsg());
        }
    }

    @Override
    public RefundResult refund(RefundCommand cmd) {
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(cmd.payNo());
        model.setRefundAmount(PayAmounts.toYuanText(cmd.amount()));
        // 部分退款必须传退款请求号；用售后单号，重复请求时支付宝按它幂等
        model.setOutRequestNo(cmd.refundNo());
        model.setRefundReason("订单 " + cmd.orderNo() + " 退款");
        AlipayTradeRefundRequest req = new AlipayTradeRefundRequest();
        req.setBizModel(model);
        AlipayTradeRefundResponse r = execute(req, "支付宝退款");
        if (!r.isSuccess()) {
            log.warn("支付宝退款失败 payNo={} refundNo={} code={} msg={}", cmd.payNo(), cmd.refundNo(),
                    r.getSubCode(), r.getSubMsg());
            throw new BusinessException("支付宝退款失败：" + r.getSubMsg());
        }
        log.info("支付宝退款已提交 payNo={} refundNo={} amount={} fundChange={}", cmd.payNo(), cmd.refundNo(),
                cmd.amount(), r.getFundChange());
        if ("Y".equals(r.getFundChange())) {
            return RefundResult.success();
        }
        // fund_change = N：同一退款请求号重复提交（已退过）或处理中，以退款查询为准
        return queryRefund(cmd.payNo(), cmd.refundNo());
    }

    @Override
    public RefundResult queryRefund(String payNo, String refundNo) {
        AlipayTradeFastpayRefundQueryModel model = new AlipayTradeFastpayRefundQueryModel();
        model.setOutTradeNo(payNo);
        model.setOutRequestNo(refundNo);
        model.setQueryOptions(List.of("gmt_refund_pay"));
        AlipayTradeFastpayRefundQueryRequest req = new AlipayTradeFastpayRefundQueryRequest();
        req.setBizModel(model);
        AlipayTradeFastpayRefundQueryResponse r = execute(req, "支付宝退款查询");
        if (!r.isSuccess()) {
            throw new BusinessException("支付宝退款查询失败：" + r.getSubMsg());
        }
        // 官方说明：返回了退款数据且 refund_status 为空或 REFUND_SUCCESS 即退款成功；没有数据说明还未成功
        if (StringUtils.hasText(r.getOutRequestNo())
                && (!StringUtils.hasText(r.getRefundStatus()) || REFUND_SUCCESS.equals(r.getRefundStatus()))) {
            return RefundResult.success();
        }
        return RefundResult.processing();
    }

    /**
     * 异步通知：先用支付宝公钥验签，再核对 app_id，金额由结算逻辑与流水比对。
     * 只处理 TRADE_SUCCESS / TRADE_FINISHED 为已支付；退款引起的通知（TRADE_CLOSED、带 refund_fee）不改变支付结果。
     */
    public ChannelTrade parseNotify(Map<String, String> params) {
        PaySettings.Alipay s = settings.alipay();
        if (s.missing() != null) {
            throw new PayNotifyException("支付宝未配置");
        }
        if (!verifier.verify(params, s.alipayPublicKey())) {
            throw new PayNotifyException("支付宝通知验签失败 outTradeNo=" + params.get("out_trade_no"));
        }
        if (!s.appId().equals(params.get("app_id"))) {
            throw new PayNotifyException("支付宝通知 app_id 与配置不符 outTradeNo=" + params.get("out_trade_no"));
        }
        String tradeNo = params.get("trade_no");
        return new ChannelTrade(params.get("out_trade_no"), toState(params.get("trade_status")),
                tradeNo == null ? "" : tradeNo, PayAmounts.yuanTextToFen(params.get("total_amount")));
    }

    static ChannelTrade.State toState(String tradeStatus) {
        if (tradeStatus == null) {
            return ChannelTrade.State.WAITING;
        }
        return switch (tradeStatus) {
            case "TRADE_SUCCESS", "TRADE_FINISHED" -> ChannelTrade.State.PAID;
            case "TRADE_CLOSED" -> ChannelTrade.State.CLOSED;
            default -> ChannelTrade.State.WAITING;
        };
    }

    private <T extends com.alipay.api.AlipayResponse> T execute(com.alipay.api.AlipayRequest<T> req, String action) {
        try {
            return require().execute(req);
        } catch (AlipayApiException e) {
            log.warn("{}异常：{}", action, e.getErrMsg());
            throw new BusinessException(action + "失败，请稍后重试");
        }
    }

    private AlipayClient require() {
        PaySettings.Alipay s = settings.alipay();
        if (s.missing() != null) {
            throw new BusinessException("支付宝未配置：" + s.missing());
        }
        return clients.get(s).orElseThrow(() -> new BusinessException("支付宝暂不可用，请检查应用配置"));
    }
}

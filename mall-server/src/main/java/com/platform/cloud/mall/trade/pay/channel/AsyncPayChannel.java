package com.platform.cloud.mall.trade.pay.channel;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.trade.pay.PayType;
import com.platform.cloud.mall.trade.pay.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 异步支付渠道（微信、支付宝）：发起时只在渠道「下单」拿到调起参数，资金结果以回调或主动查单为准。
 * <p>
 * 所有方法都直接访问渠道网络接口，调用方不要在持有数据库事务时调用下单 / 查单（退款例外，见 PayChannel）。
 */
public interface AsyncPayChannel extends PayChannel {

    /** 某种发起方式当前是否可用（开关、配置完整性、SDK 初始化） */
    Availability availability(PayType payType);

    /**
     * 渠道下单，返回前端调起参数（JSAPI 为 wx.chooseWXPay / WeixinJSBridge 参数，H5 为 h5Url，支付宝为 payUrl）。
     * 失败抛 BusinessException，信息可直接展示。
     */
    Map<String, String> prepay(PrepayCommand cmd);

    /** 按商户订单号（payNo）查单 */
    ChannelTrade query(Payment payment);

    /** 关闭渠道订单，防止用户在旧的调起页面继续付款；已支付时抛异常 */
    void close(Payment payment);

    /** 查询退款结果（补偿任务用） */
    RefundResult queryRefund(String payNo, String refundNo);

    /** 异步渠道必须走收银台（CashierService）的 prepay，PayService.pay 只处理同步渠道 */
    @Override
    default PayResult pay(PayCommand cmd) {
        throw new BusinessException("请通过收银台发起支付");
    }

    /**
     * @param subject   商品描述，展示在用户的支付账单里
     * @param expireAt  渠道订单失效时间，取订单的支付截止时间：过了这个点渠道直接拒绝付款，从源头减少「关单后到账」
     * @param openid    JSAPI 必填：会员在支付公众号下的 openid
     * @param clientIp  H5 必填：用户终端 IP，微信用于风控
     * @param returnUrl 支付完成后跳回的商城页面，可空
     */
    record PrepayCommand(PayType payType, String payNo, String orderNo, BigDecimal amount, String subject,
                         LocalDateTime expireAt, String openid, String clientIp, String returnUrl) {
    }

    /**
     * @param on     后台开关是否打开；关闭的方式不在收银台展示
     * @param ready  配置完整且可以下单
     * @param reason 不可用原因（给买家看的提示）
     */
    record Availability(boolean on, boolean ready, String reason) {

        public static Availability off() {
            return new Availability(false, false, "未开启");
        }

        public static Availability notReady(String reason) {
            return new Availability(true, false, reason);
        }

        public static Availability available() {
            return new Availability(true, true, "");
        }
    }
}

package com.platform.cloud.mall.trade.pay.service;

import com.platform.cloud.mall.trade.pay.vo.PayChannelVO;
import com.platform.cloud.mall.trade.pay.vo.PayLaunchVO;
import com.platform.cloud.mall.trade.pay.vo.PayStatusVO;

import java.util.List;

/**
 * 商城端收银台：可用支付方式、发起支付、支付结果查询。
 * 同步渠道的扣款与落账仍由 {@link PayService#pay} 完成，本服务负责异步渠道的下单与支付单管理。
 */
public interface CashierService {

    /**
     * 当前订单可用的支付方式。
     *
     * @param inWechat 前端判断是否在微信内置浏览器（UA 含 MicroMessenger），影响微信 / 支付宝的可用性
     */
    List<PayChannelVO> channels(Long memberId, Long orderId, boolean inWechat);

    /**
     * @param payPassword 余额支付密码，payType=BALANCE 时必填，在扣余额之前校验；其他方式忽略
     * @param clientIp    用户终端 IP，微信 H5 支付必填
     */
    PayLaunchVO pay(Long memberId, Long orderId, String payType, String payPassword, String clientIp);

    /** 支付结果；本地仍未支付时主动向渠道查单补偿（同一支付单 3 秒内只查一次） */
    PayStatusVO payStatus(Long memberId, Long orderId);
}

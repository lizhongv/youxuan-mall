package com.platform.cloud.mall.trade.pay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 发起支付结果。同步渠道（余额、模拟）paid = true；异步渠道 paid = false，前端按 payType 用 payParams 调起支付，
 * 返回后轮询 GET /app/order/{id}/pay-status。
 */
@Data
public class PayLaunchVO {

    private Boolean paid;
    private String payType;
    @Schema(description = "支付单号（异步渠道）")
    private String payNo;
    @Schema(description = "WECHAT_JSAPI：{appId,timeStamp,nonceStr,package,signType,paySign}；"
            + "WECHAT_H5：{h5Url}；ALIPAY_WAP：{payUrl}")
    private Map<String, String> payParams;

    public static PayLaunchVO paid(String payType) {
        PayLaunchVO vo = new PayLaunchVO();
        vo.setPaid(true);
        vo.setPayType(payType);
        return vo;
    }

    public static PayLaunchVO pending(String payType, String payNo, Map<String, String> params) {
        PayLaunchVO vo = new PayLaunchVO();
        vo.setPaid(false);
        vo.setPayType(payType);
        vo.setPayNo(payNo);
        vo.setPayParams(params);
        return vo;
    }
}

package com.platform.cloud.mall.trade.pay.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 收银台可选支付方式 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayChannelVO {

    @Schema(description = "BALANCE / WECHAT_JSAPI / WECHAT_H5 / ALIPAY_WAP / MOCK，即 pay 接口的 payType")
    private String type;
    private String name;
    @Schema(description = "图标名：balance / wechat / alipay / mock，前端映射到本地图标")
    private String icon;
    @Schema(description = "当前能否选择；false 时置灰并展示 reason")
    private Boolean enabled;
    private String reason;
}

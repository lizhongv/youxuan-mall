package com.platform.cloud.mall.trade.order.vo;

import com.platform.cloud.mall.marketing.api.CouponApi.CouponOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 结算预览出参 */
@Data
@Schema(description = "结算预览")
public class OrderConfirmVO {

    private List<Item> items;
    @Schema(description = "收货地址：传了 addressId 用该地址；未传时回退到默认地址（无默认取最近修改的），会员没有地址时为 null")
    private AddressVO address;
    private BigDecimal goodsAmount;
    private BigDecimal freightAmount;
    private BigDecimal couponAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    @Schema(description = "本次使用的会员券 ID，null 表示不用券")
    private Long memberCouponId;
    @Schema(description = "可选优惠券（可用的在前）；秒杀订单为空")
    private List<CouponOption> coupons;
    @Schema(description = "会员余额，前端判断余额支付是否可用")
    private BigDecimal balance;

    @Data
    public static class Item {
        private Long goodsId;
        private Long skuId;
        private String goodsName;
        private String pic;
        private String specText;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal totalAmount;
    }

    @Data
    public static class AddressVO {
        private Long id;
        private String name;
        private String mobile;
        private String province;
        private String city;
        private String district;
        private String detail;
    }
}

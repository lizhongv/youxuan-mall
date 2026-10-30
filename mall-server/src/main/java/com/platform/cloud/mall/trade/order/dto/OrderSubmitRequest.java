package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 结算预览 / 提交订单入参（api.md OrderSubmit） */
@Data
@Schema(description = "结算 / 下单入参")
public class OrderSubmitRequest {

    public static final String SOURCE_CART = "CART";
    public static final String SOURCE_BUY_NOW = "BUY_NOW";

    @NotBlank(message = "下单来源不能为空")
    @Pattern(regexp = "CART|BUY_NOW", message = "下单来源只能是 CART 或 BUY_NOW")
    @Schema(description = "CART 购物车结算 / BUY_NOW 立即购买")
    private String source;

    @Size(max = 100, message = "一次最多结算 100 个购物车商品")
    @Schema(description = "购物车项 ID（source=CART）")
    private List<Long> cartIds;

    @Valid
    @Size(max = 100, message = "一次最多购买 100 种商品")
    @Schema(description = "购买商品（source=BUY_NOW）")
    private List<Item> items;

    @Schema(description = "秒杀活动 ID，立即购买秒杀商品时传")
    private Long seckillId;

    @Schema(description = "收货地址 ID；提交订单时必填，结算预览可空")
    private Long addressId;

    @Schema(description = "会员券 ID；confirm 不传自动选最优，传 0 不用券")
    private Long memberCouponId;

    @Size(max = 200, message = "留言不能超过 200 字")
    @Schema(description = "买家留言")
    private String remark;

    @Data
    public static class Item {
        @NotNull(message = "skuId 不能为空")
        private Long skuId;

        @NotNull(message = "数量不能为空")
        @Min(value = 1, message = "数量至少为 1")
        @Max(value = 99, message = "单个商品最多购买 99 件")
        private Integer quantity;
    }
}

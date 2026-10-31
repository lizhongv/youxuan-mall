package com.platform.cloud.mall.goods.api;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * 商品领域对其他领域开放的能力：SKU 快照、库存扣减与回补、商品概要。
 */
public interface GoodsApi {

    /** 按 SKU ID 批量取快照，含 SPU 名称、类目、上下架状态；不存在的 ID 不返回 */
    Map<Long, SkuSnapshot> getSkus(Collection<Long> skuIds);

    /** 批量取商品概要（收藏、足迹、优惠券适用商品展示） */
    Map<Long, GoodsBrief> getGoodsBriefs(Collection<Long> goodsIds);

    /**
     * 扣减库存并增加销量。用 {@code UPDATE ... SET stock = stock - ? WHERE id = ? AND stock >= ?} 保证不超卖，
     * 影响行数为 0 时抛业务异常「xxx 库存不足」。必须在调用方事务内调用。
     */
    void deductStock(Long skuId, int quantity);

    /** 回补库存并扣减销量（取消订单、关单、退款） */
    void restoreStock(Long skuId, int quantity);

    /**
     * 运费计算用：SKU 所属商品的运费模板与单件重量（kg）。不存在的 ID 不返回。
     * 单独成方法而不是扩展 {@link SkuSnapshot}，避免影响购物车、秒杀等只关心价格库存的调用方。
     */
    Map<Long, ShippingInfo> getShipping(Collection<Long> skuIds);

    /** 引用某运费模板的商品数（删除模板前校验） */
    long countByFreightTemplate(Long templateId);

    /** 统计用：在售商品数 */
    long countOnSale();

    record SkuSnapshot(Long skuId, Long goodsId, Long categoryId, String goodsName, String pic, String specText,
                       BigDecimal price, Integer stock, boolean onSale) {
    }

    /** @param freightTemplateId 商品指定的运费模板，空表示用默认模板 */
    record ShippingInfo(Long skuId, Long freightTemplateId, BigDecimal weight) {
    }

    record GoodsBrief(Long id, String name, String pic, BigDecimal price, BigDecimal originalPrice, Integer sales,
                      boolean onSale) {
    }

    /** 下单商品行，供优惠券判断适用范围 */
    record GoodsLine(Long goodsId, Long categoryId, BigDecimal amount) {
    }
}

package com.platform.cloud.mall.goods.vo;

import com.platform.cloud.mall.goods.entity.SpecItem;
import com.platform.cloud.mall.marketing.vo.SeckillVO;
import com.platform.cloud.mall.trade.api.TradeApi.ReviewBrief;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城端商品详情。不暴露虚拟销量等后台字段；sales 为展示销量。
 */
@Data
@Schema(description = "商品详情（商城端）")
public class AppGoodsDetailVO {

    private Long id;
    private Long categoryId;
    private Long brandId;
    private String brandName;
    private String name;
    private String subtitle;
    private String pic;
    private List<String> album;
    private String detail;
    private String unit;
    private String keywords;
    private String status;
    private Integer isNew;
    private Integer isHot;
    private Integer isRecommend;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer stock;
    @Schema(description = "展示销量 = 真实销量 + 虚拟销量")
    private Integer sales;
    private List<SpecItem> specItems;
    private List<SkuVO> skus;

    @Schema(description = "评价总数")
    private Long reviewCount;
    @Schema(description = "好评率（百分比整数）")
    private Integer goodRate;
    @Schema(description = "最新 2 条评价")
    private List<ReviewBrief> latestReviews;

    @Schema(description = "是否已收藏；未登录时不返回")
    private Boolean favorited;

    @Schema(description = "进行中的秒杀活动，可空")
    private SeckillVO seckill;
}

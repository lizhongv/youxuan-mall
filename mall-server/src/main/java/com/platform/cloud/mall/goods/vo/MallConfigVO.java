package com.platform.cloud.mall.goods.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商城端公开参数。
 */
@Data
@Schema(description = "商城公开参数")
public class MallConfigVO {

    private String mallName;
    private String servicePhone;
    @Schema(description = "基础运费")
    private BigDecimal freightAmount;
    @Schema(description = "满多少包邮，0 表示一律包邮")
    private BigDecimal freightFreeThreshold;
}

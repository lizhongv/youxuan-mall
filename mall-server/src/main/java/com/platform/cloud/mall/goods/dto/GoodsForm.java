package com.platform.cloud.mall.goods.dto;

import com.platform.cloud.mall.goods.entity.SpecItem;
import com.platform.cloud.mall.goods.support.GoodsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品新增 / 修改表单。SPU 的价格、库存不由前端传，保存时从 SKU 汇总，避免两边不一致。
 */
@Data
@Schema(description = "商品表单")
public class GoodsForm {

    @NotNull(message = "请选择类目")
    private Long categoryId;

    private Long brandId;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 128, message = "商品名称最多 128 个字")
    private String name;

    @Size(max = 255, message = "副标题最多 255 个字")
    private String subtitle;

    @NotBlank(message = "请上传商品主图")
    @Size(max = 255, message = "主图地址过长")
    private String pic;

    @Size(max = 10, message = "轮播图最多 10 张")
    private List<String> album = new ArrayList<>();

    private String detail;

    @Size(max = 16, message = "单位最多 16 个字")
    private String unit;

    @Size(max = 255, message = "关键词最多 255 个字")
    private String keywords;

    @Min(value = 0, message = "虚拟销量不能小于 0")
    private Integer virtualSales;

    private Integer sort;

    @Pattern(regexp = GoodsStatus.PATTERN, message = "状态只能是 ON_SALE 或 OFF_SALE")
    private String status;

    @Min(0) @Max(1)
    private Integer isNew;

    @Min(0) @Max(1)
    private Integer isHot;

    @Min(0) @Max(1)
    private Integer isRecommend;

    @Valid
    @Schema(description = "规格定义，无规格商品传空数组")
    private List<SpecItem> specItems = new ArrayList<>();

    @Valid
    @NotEmpty(message = "至少需要一个 SKU")
    private List<SkuForm> skus = new ArrayList<>();

    /** SKU 表单。修改商品时带 id 表示更新原 SKU，不带表示新增；原有但未提交的 SKU 会被删除。 */
    @Data
    @Schema(description = "SKU 表单")
    public static class SkuForm {

        @Schema(description = "SKU ID，新增时为空")
        private Long id;

        @Schema(description = "与 specItems 顺序一致的规格值")
        private List<String> specValues = new ArrayList<>();

        @Size(max = 64, message = "商家编码最多 64 个字")
        private String code;

        @Size(max = 255, message = "SKU 图片地址过长")
        private String pic;

        @NotNull(message = "SKU 价格不能为空")
        private BigDecimal price;

        private BigDecimal originalPrice;

        @NotNull(message = "SKU 库存不能为空")
        private Integer stock;
    }
}

package com.platform.cloud.mall.marketing.dto;

import com.platform.cloud.mall.marketing.support.CouponScope;
import com.platform.cloud.mall.marketing.support.CouponType;
import com.platform.cloud.mall.marketing.support.CouponValidType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 优惠券模板新增 / 修改。字段与 mkt_coupon 一致；类型相关的组合校验在 service 中做。
 */
@Data
@Schema(description = "优惠券表单")
public class CouponRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 64, message = "名称最多 64 个字")
    private String name;

    @NotBlank(message = "类型不能为空")
    @Pattern(regexp = CouponType.PATTERN, message = "类型只能是 FULL_REDUCTION / DISCOUNT / NO_THRESHOLD")
    private String type;

    @DecimalMin(value = "0", message = "门槛不能小于 0")
    @Schema(description = "使用门槛，适用商品金额 ≥ 该值可用；无门槛券忽略")
    private BigDecimal threshold;

    @DecimalMin(value = "0", message = "减免金额不能小于 0")
    @Schema(description = "减免金额（满减 / 无门槛）")
    private BigDecimal amount;

    @DecimalMin(value = "0", message = "折扣率不能小于 0")
    @DecimalMax(value = "1", message = "折扣率不能大于 1")
    @Schema(description = "折扣率（折扣券），0.80 即 8 折")
    private BigDecimal discount;

    @DecimalMin(value = "0", message = "最高减免不能小于 0")
    @Schema(description = "折扣券最高减免，0 不限")
    private BigDecimal maxAmount;

    @Pattern(regexp = CouponScope.PATTERN, message = "范围只能是 ALL / CATEGORY / GOODS")
    private String scope;

    @Size(max = 500, message = "适用对象最多 500 个")
    @Schema(description = "适用的类目或商品 ID")
    private List<Long> scopeIds;

    @NotNull(message = "发行量不能为空")
    @Min(value = 0, message = "发行量不能小于 0")
    @Schema(description = "发行量，0 不限")
    private Integer total;

    @Min(value = 1, message = "每人限领至少 1 张")
    @Max(value = 100, message = "每人限领最多 100 张")
    private Integer perLimit;

    @NotBlank(message = "有效期类型不能为空")
    @Pattern(regexp = CouponValidType.PATTERN, message = "有效期类型只能是 FIXED / DAYS")
    private String validType;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Min(value = 0, message = "有效天数不能小于 0")
    @Max(value = 3650, message = "有效天数过大")
    private Integer validDays;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;

    @Size(max = 255, message = "说明最多 255 个字")
    private String description;
}

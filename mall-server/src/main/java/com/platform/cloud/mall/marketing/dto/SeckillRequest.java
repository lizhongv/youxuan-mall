package com.platform.cloud.mall.marketing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动新增 / 修改。
 */
@Data
@Schema(description = "秒杀活动表单")
public class SeckillRequest {

    @NotBlank(message = "活动标题不能为空")
    @Size(max = 64, message = "活动标题最多 64 个字")
    private String title;

    @NotNull(message = "请选择商品")
    private Long goodsId;

    @NotNull(message = "请选择 SKU")
    private Long skuId;

    @NotNull(message = "秒杀价不能为空")
    @DecimalMin(value = "0.01", message = "秒杀价至少 0.01")
    private BigDecimal seckillPrice;

    @NotNull(message = "活动库存不能为空")
    @Min(value = 1, message = "活动库存至少 1")
    private Integer stock;

    @Min(value = 1, message = "每人限购至少 1 件")
    @Max(value = 99, message = "每人限购最多 99 件")
    private Integer limitPerMember;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startAt;

    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endAt;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}

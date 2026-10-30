package com.platform.cloud.mall.goods.dto;

import com.platform.cloud.mall.goods.support.GoodsStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量上下架。
 */
@Data
@Schema(description = "批量上下架")
public class GoodsStatusRequest {

    /** 一次最多 200 个，与分页上限一致，防止误操作全表 */
    @NotEmpty(message = "请选择商品")
    @Size(max = 200, message = "一次最多操作 200 个商品")
    private List<Long> ids;

    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = GoodsStatus.PATTERN, message = "状态只能是 ON_SALE 或 OFF_SALE")
    private String status;
}

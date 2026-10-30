package com.platform.cloud.mall.trade.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 勾选 / 取消勾选 */
@Data
public class CartCheckRequest {

    @Schema(description = "购物车项 ID，为空表示全部")
    private List<Long> ids;

    @NotNull(message = "checked 不能为空")
    private Boolean checked;
}

package com.platform.cloud.mall.trade.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 发货入参；快递公司取字典 express_company 的名称，存字符串便于以后自由扩展 */
@Data
public class OrderDeliverRequest {

    @NotBlank(message = "请选择快递公司")
    @Size(max = 64, message = "快递公司不能超过 64 字")
    @Schema(description = "快递公司")
    private String deliveryCompany;

    @NotBlank(message = "请填写快递单号")
    @Size(max = 64, message = "快递单号不能超过 64 字")
    @Schema(description = "快递单号")
    private String deliveryNo;
}

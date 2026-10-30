package com.platform.cloud.mall.trade.aftersale.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 申请售后 */
@Data
public class AftersaleApplyRequest {

    @NotNull(message = "请选择售后商品")
    private Long orderItemId;

    @NotBlank(message = "请选择售后类型")
    @Pattern(regexp = "REFUND|RETURN_REFUND", message = "售后类型只能是 REFUND 或 RETURN_REFUND")
    @Schema(description = "REFUND 仅退款 / RETURN_REFUND 退货退款")
    private String type;

    @NotBlank(message = "请选择售后原因")
    @Size(max = 64, message = "原因不能超过 64 字")
    @Schema(description = "售后原因（字典 aftersale_reason）")
    private String reason;

    @Size(max = 500, message = "说明不能超过 500 字")
    private String description;

    @Size(max = 9, message = "最多上传 9 张图片")
    private List<@NotBlank @Size(max = 255) String> images;

    @NotNull(message = "请填写退款金额")
    @DecimalMin(value = "0.01", message = "退款金额至少 0.01")
    @Digits(integer = 8, fraction = 2, message = "金额最多两位小数")
    @Schema(description = "退款金额，不超过该商品实付减已退金额")
    private BigDecimal amount;
}

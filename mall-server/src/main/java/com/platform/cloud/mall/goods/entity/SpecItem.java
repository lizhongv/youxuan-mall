package com.platform.cloud.mall.goods.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 规格项，如 {"name":"颜色","values":["红","蓝"]}。以 JSON 存在 gds_goods.spec_items，
 * 同时作为表单入参，避免两套几乎一样的结构来回拷贝。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "规格项")
public class SpecItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "规格名不能为空")
    @Size(max = 32, message = "规格名最多 32 个字")
    @Schema(description = "规格名，如 颜色")
    private String name;

    @NotEmpty(message = "规格值不能为空")
    @Schema(description = "规格值")
    private List<String> values = new ArrayList<>();
}

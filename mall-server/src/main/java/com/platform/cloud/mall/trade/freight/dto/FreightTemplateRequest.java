package com.platform.cloud.mall.trade.freight.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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

/** 运费模板新增 / 修改。规则与包邮条件整体提交、整体替换。 */
@Data
@Schema(description = "运费模板表单")
public class FreightTemplateRequest {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 64, message = "模板名称最多 64 个字")
    private String name;

    @NotBlank(message = "请选择计费方式")
    @Pattern(regexp = "BY_COUNT|BY_WEIGHT", message = "计费方式只能是 BY_COUNT 或 BY_WEIGHT")
    @Schema(description = "BY_COUNT 按件 / BY_WEIGHT 按重量（kg）")
    private String chargeType;

    @Min(0) @Max(1)
    @Schema(description = "是否默认模板：设为 1 时其他模板自动取消默认")
    private Integer isDefault;

    @Min(0) @Max(1)
    private Integer status;

    @Valid
    @NotEmpty(message = "至少需要一条计费规则")
    @Size(max = 50, message = "计费规则最多 50 条")
    private List<RuleForm> rules = new ArrayList<>();

    @Valid
    @Size(max = 50, message = "包邮条件最多 50 条")
    private List<FreeForm> frees = new ArrayList<>();

    /** 计费规则：provinces 为空表示默认地区（全国其他地区） */
    @Data
    public static class RuleForm {

        @Schema(description = "适用省份名，如 [\"广东省\",\"广西壮族自治区\"]；空数组表示默认地区")
        private List<String> provinces = new ArrayList<>();

        @NotNull(message = "首件 / 首重不能为空")
        @DecimalMin(value = "0.001", message = "首件 / 首重必须大于 0")
        private BigDecimal firstUnit;

        @NotNull(message = "首费不能为空")
        @DecimalMin(value = "0", message = "首费不能小于 0")
        private BigDecimal firstFee;

        @DecimalMin(value = "0", message = "续件 / 续重不能小于 0")
        @Schema(description = "续件 / 续重，0 表示不再续费")
        private BigDecimal nextUnit;

        @DecimalMin(value = "0", message = "续费不能小于 0")
        private BigDecimal nextFee;
    }

    /** 指定条件包邮：满 minCount 件或满 minAmount 元（任一满足即可） */
    @Data
    public static class FreeForm {

        @Schema(description = "适用省份名；空数组表示全部地区")
        private List<String> provinces = new ArrayList<>();

        @Min(value = 0, message = "包邮件数不能小于 0")
        @Schema(description = "满 N 件包邮，0 表示不按件")
        private Integer minCount;

        @DecimalMin(value = "0", message = "包邮金额不能小于 0")
        @Schema(description = "满 X 元包邮（会员折扣后金额），0 表示不按金额")
        private BigDecimal minAmount;
    }
}

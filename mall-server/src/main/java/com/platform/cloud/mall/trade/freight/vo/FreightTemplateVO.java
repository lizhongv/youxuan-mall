package com.platform.cloud.mall.trade.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 运费模板（详情含规则与包邮条件；分页列表同样返回，便于列表直接展示规则摘要）。 */
@Data
@Schema(description = "运费模板")
public class FreightTemplateVO {

    private Long id;
    private String name;
    private String chargeType;
    private Integer isDefault;
    private Integer status;
    private List<RuleVO> rules;
    private List<FreeVO> frees;
    private LocalDateTime createdAt;

    @Data
    public static class RuleVO {
        private List<String> provinces;
        private BigDecimal firstUnit;
        private BigDecimal firstFee;
        private BigDecimal nextUnit;
        private BigDecimal nextFee;
    }

    @Data
    public static class FreeVO {
        private List<String> provinces;
        private Integer minCount;
        private BigDecimal minAmount;
    }
}

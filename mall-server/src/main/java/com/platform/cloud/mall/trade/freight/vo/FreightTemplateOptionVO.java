package com.platform.cloud.mall.trade.freight.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 运费模板下拉项（商品表单选择模板用） */
@Schema(description = "运费模板选项")
public record FreightTemplateOptionVO(Long id, String name, String chargeType, Integer isDefault) {
}

package com.platform.cloud.mall.trade.aftersale.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 商家审核售后 */
@Data
public class AftersaleAuditRequest {

    @Size(max = 255, message = "审核备注不能超过 255 字")
    private String remark;
}

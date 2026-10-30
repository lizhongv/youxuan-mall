package com.platform.cloud.mall.trade.aftersale.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 买家填写退货物流 */
@Data
public class AftersaleReturnRequest {

    @NotBlank(message = "请选择快递公司")
    @Size(max = 64, message = "快递公司不能超过 64 字")
    private String returnCompany;

    @NotBlank(message = "请填写快递单号")
    @Size(max = 64, message = "快递单号不能超过 64 字")
    private String returnNo;
}

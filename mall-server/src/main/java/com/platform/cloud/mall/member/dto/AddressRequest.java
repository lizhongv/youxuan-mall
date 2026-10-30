package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增 / 修改收货地址 */
@Data
public class AddressRequest {

    @NotBlank
    @Size(max = 32)
    private String name;

    /** 收货人可能用座机，允许 7~20 位数字和短横线 */
    @NotBlank
    @Pattern(regexp = "^[0-9-]{7,20}$", message = "联系电话格式不正确")
    private String mobile;

    @NotBlank
    @Size(max = 32)
    private String province;

    @NotBlank
    @Size(max = 32)
    private String city;

    @NotBlank
    @Size(max = 32)
    private String district;

    @NotBlank
    @Size(max = 255)
    private String detail;

    @Min(0)
    @Max(1)
    private Integer isDefault;
}

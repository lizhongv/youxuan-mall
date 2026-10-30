package com.platform.cloud.mall.member.vo;

import lombok.Data;

/** 收货地址 */
@Data
public class AddressVO {

    private Long id;
    private String name;
    private String mobile;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
}

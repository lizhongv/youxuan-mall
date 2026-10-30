package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 收货地址 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_address")
public class MbrAddress extends BaseEntity {

    private Long memberId;
    private String name;
    private String mobile;
    private String province;
    private String city;
    private String district;
    private String detail;
    private Integer isDefault;
}

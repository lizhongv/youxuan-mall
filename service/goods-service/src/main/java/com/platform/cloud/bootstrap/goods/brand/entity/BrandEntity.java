package com.platform.cloud.bootstrap.goods.brand.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_brand")
public class BrandEntity extends BaseEntity {

    private String name;
    private String logoUrl;
    private String description;
    private Integer sortOrder;
    private Integer visible;
}

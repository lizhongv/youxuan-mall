package com.platform.cloud.bootstrap.goods.category.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_category")
public class CategoryEntity extends BaseEntity {

    private Long parentId;
    private String name;
    private String slug;
    private String iconUrl;
    private String bannerUrl;
    private Integer sortOrder;
    private Integer visible;
}

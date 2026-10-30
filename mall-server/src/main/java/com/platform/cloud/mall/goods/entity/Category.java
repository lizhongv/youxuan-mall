package com.platform.cloud.mall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品类目。最多两级：parentId = 0 为一级，商品只能挂在二级类目下，便于前台按一级聚合展示。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gds_category")
public class Category extends BaseEntity {

    /** 一级类目的父 ID 约定为 0 而不是 NULL，查询条件更简单 */
    public static final long ROOT_ID = 0L;

    private Long parentId;
    private String name;
    private String icon;
    private Integer sort;
    private Integer status;
}

package com.platform.cloud.mall.goods.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 品牌。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("gds_brand")
public class Brand extends BaseEntity {

    private String name;
    private String logo;
    /** 首字母，后台按字母索引筛选品牌 */
    private String firstLetter;
    private String description;
    private Integer sort;
    private Integer status;
}

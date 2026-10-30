package com.platform.cloud.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 轮播广告。linkType 决定前端点击后跳转商品、类目还是外链。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_banner")
public class Banner extends BaseEntity {

    private String title;
    private String image;
    private String linkType;
    private String linkValue;
    private String position;
    private Integer sort;
    private Integer status;
}

package com.platform.cloud.mall.marketing.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商城公告。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mkt_notice")
public class Notice extends BaseEntity {

    private String title;
    private String content;
    private Integer status;
    private Integer sort;
}

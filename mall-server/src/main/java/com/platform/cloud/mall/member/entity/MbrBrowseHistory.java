package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 浏览足迹：同一商品只保留一条，updated_at 即最近浏览时间 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_browse_history")
public class MbrBrowseHistory extends BaseEntity {

    private Long memberId;
    private Long goodsId;
}

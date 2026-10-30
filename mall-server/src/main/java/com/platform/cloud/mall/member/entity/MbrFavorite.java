package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 商品收藏 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_favorite")
public class MbrFavorite extends BaseEntity {

    private Long memberId;
    private Long goodsId;
}

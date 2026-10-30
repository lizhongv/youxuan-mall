package com.platform.cloud.mall.trade.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.trade.review.entity.Review;
import com.platform.cloud.mall.trade.review.mapper.row.ReviewStatRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /** 一条 SQL 出全部分档计数，避免商品详情页连查 5 次；只统计显示中的评价 */
    @Select("SELECT COUNT(*) AS total, "
            + "COALESCE(SUM(CASE WHEN rating >= 4 THEN 1 ELSE 0 END), 0) AS good, "
            + "COALESCE(SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END), 0) AS medium, "
            + "COALESCE(SUM(CASE WHEN rating <= 2 THEN 1 ELSE 0 END), 0) AS bad, "
            + "COALESCE(SUM(CASE WHEN images IS NOT NULL AND JSON_LENGTH(images) > 0 THEN 1 ELSE 0 END), 0) AS image "
            + "FROM trd_review WHERE deleted = 0 AND status = 1 AND goods_id = #{goodsId}")
    ReviewStatRow statByGoods(@Param("goodsId") Long goodsId);
}

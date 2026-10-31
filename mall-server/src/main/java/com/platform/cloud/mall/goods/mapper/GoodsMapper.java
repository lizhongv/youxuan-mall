package com.platform.cloud.mall.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.platform.cloud.mall.goods.entity.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 SPU。
 */
@Mapper
public interface GoodsMapper extends BaseMapper<Goods> {

    /**
     * 商城端在售商品搜索。销量排序要按「真实 + 虚拟」表达式排序，Wrapper 不方便表达，
     * 因此用注解 SQL；排序方式只在 choose 分支里选择固定片段，不拼接任何入参。
     * 只查列表需要的列，避开大字段 detail。
     */
    @Select("""
            <script>
            SELECT id, name, subtitle, pic, price, original_price, sales, virtual_sales,
                   is_new, is_hot, is_recommend, sort, created_at
            FROM gds_goods
            WHERE deleted = 0 AND status = 'ON_SALE'
            <if test="keyword != null and keyword != ''">
              AND (name LIKE CONCAT('%', #{keyword}, '%')
                   OR subtitle LIKE CONCAT('%', #{keyword}, '%')
                   OR keywords LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="categoryIds != null and categoryIds.size() > 0">
              AND category_id IN
              <foreach collection="categoryIds" item="cid" open="(" separator="," close=")">#{cid}</foreach>
            </if>
            <if test="brandId != null">
              AND brand_id = #{brandId}
            </if>
            ORDER BY
            <choose>
              <when test="sort == 'SALES'">(sales + virtual_sales) DESC, id DESC</when>
              <when test="sort == 'PRICE_ASC'">price ASC, id DESC</when>
              <when test="sort == 'PRICE_DESC'">price DESC, id DESC</when>
              <when test="sort == 'NEW'">created_at DESC, id DESC</when>
              <otherwise>is_recommend DESC, sort ASC, id DESC</otherwise>
            </choose>
            </script>
            """)
    IPage<Goods> pageOnSale(IPage<Goods> page, @Param("keyword") String keyword,
                            @Param("categoryIds") List<Long> categoryIds, @Param("brandId") Long brandId,
                            @Param("sort") String sort);

    /** 下单扣减后同步 SPU 冗余库存与销量；SKU 层已保证不超卖，这里用 GREATEST 兜底防止冗余值为负 */
    @Update("UPDATE gds_goods SET stock = GREATEST(stock - #{quantity}, 0), sales = sales + #{quantity} "
            + "WHERE id = #{id}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** 后台编辑 SKU 后按 SKU 当前库存重算 SPU 冗余库存，不信任表单值 */
    @Update("UPDATE gds_goods SET stock = (SELECT COALESCE(SUM(s.stock), 0) FROM gds_sku s "
            + "WHERE s.goods_id = #{id} AND s.deleted = 0) WHERE id = #{id}")
    int syncStock(@Param("id") Long id);

    /** 取消 / 退款回补 SPU 冗余库存与销量 */
    @Update("UPDATE gds_goods SET stock = stock + #{quantity}, sales = GREATEST(sales - #{quantity}, 0) "
            + "WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);
}

package com.platform.cloud.mall.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.goods.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 商品 SKU。
 */
@Mapper
public interface SkuMapper extends BaseMapper<Sku> {

    /**
     * 条件扣减：stock >= ? 由数据库行锁保证并发下不超卖，影响行数 0 即库存不足。
     * 同时 version + 1，让后台编辑商品时的乐观锁能感知到库存已被订单改动。
     */
    @Update("UPDATE gds_sku SET stock = stock - #{quantity}, sales = sales + #{quantity}, version = version + 1 "
            + "WHERE id = #{id} AND deleted = 0 AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") int quantity);

    /** 回补库存；SKU 即使已被删除也回补，保证账目可追溯 */
    @Update("UPDATE gds_sku SET stock = stock + #{quantity}, sales = GREATEST(sales - #{quantity}, 0), "
            + "version = version + 1 WHERE id = #{id}")
    int restoreStock(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * 后台编辑商品时的差量调整库存：stock + delta 不得为负（编辑期间被下单买走时，减库存可能不够减）。
     * 不改销量；version + 1 与扣减保持一致。
     */
    @Update("UPDATE gds_sku SET stock = stock + #{delta}, version = version + 1 "
            + "WHERE id = #{id} AND deleted = 0 AND stock + #{delta} >= 0")
    int adjustStock(@Param("id") Long id, @Param("delta") int delta);

    /** 查 SKU 所属商品，包含已逻辑删除的 SKU（Wrapper 查询会自动带 deleted = 0，回补场景不适用） */
    @Select("SELECT goods_id FROM gds_sku WHERE id = #{id}")
    Long selectGoodsIdIncludingDeleted(@Param("id") Long id);
}

package com.platform.cloud.mall.trade.aftersale.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.cloud.mall.trade.aftersale.entity.Aftersale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface AftersaleMapper extends BaseMapper<Aftersale> {

    /** 明细已退款金额合计（含退款中，已向渠道发起的金额必须占用额度），用于计算剩余可退额度 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM trd_aftersale "
            + "WHERE deleted = 0 AND status IN ('REFUNDED', 'REFUNDING') AND order_item_id = #{orderItemId}")
    BigDecimal sumRefundedByItem(@Param("orderItemId") Long orderItemId);

    /** 订单已退款金额合计（含退款中），确认收货发积分时扣除 */
    @Select("SELECT COALESCE(SUM(amount), 0) FROM trd_aftersale "
            + "WHERE deleted = 0 AND status IN ('REFUNDED', 'REFUNDING') AND order_id = #{orderId}")
    BigDecimal sumRefundedByOrder(@Param("orderId") Long orderId);
}

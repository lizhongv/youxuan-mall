package com.platform.cloud.mall.trade.order.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 交易领域给统计领域（stats）提供的只读查询。
 * stats 不直接访问 trd_ 表，统计口径集中在交易领域维护：「已支付」= pay_time 非空且未因全额退款取消，按支付时间归属日期，金额扣除已退款。
 */
public interface TradeStatsService {

    /** [from, to) 内支付的订单销售额（实付 − 已退款）与订单数，不含支付后整单退款取消的订单 */
    SalesStat paidBetween(LocalDateTime from, LocalDateTime to);

    /** [from, to) 内按支付日期分组；没有订单的日期不返回，由调用方补 0 */
    List<DailySales> dailyPaid(LocalDateTime from, LocalDateTime to);

    /** from 之后支付订单中销量最高的商品 */
    List<GoodsSales> topGoods(LocalDateTime from, int limit);

    /** 各订单状态数量（包含 0） */
    Map<String, Long> orderStatusCount();

    /** 待商家处理的售后数：申请中 + 退货中（等待确认收货） */
    long pendingAftersale();

    record SalesStat(BigDecimal sales, long orders) {
    }

    record DailySales(String date, BigDecimal sales, long orders) {
    }

    record GoodsSales(Long goodsId, String goodsName, String pic, long quantity, BigDecimal amount) {
    }
}

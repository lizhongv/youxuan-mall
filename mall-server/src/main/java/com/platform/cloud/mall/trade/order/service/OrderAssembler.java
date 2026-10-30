package com.platform.cloud.mall.trade.order.service;

import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.entity.OrderItem;
import com.platform.cloud.mall.trade.order.entity.OrderLog;
import com.platform.cloud.mall.trade.order.enums.OrderStatus;
import com.platform.cloud.mall.trade.order.vo.OrderItemVO;
import com.platform.cloud.mall.trade.order.vo.OrderLogVO;
import com.platform.cloud.mall.trade.order.vo.OrderVO;

import java.util.List;

/** 订单实体 → VO 转换，实体不直接出现在接口上 */
public final class OrderAssembler {

    private OrderAssembler() {
    }

    public static OrderVO toVO(Order o, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(o.getId());
        vo.setOrderNo(o.getOrderNo());
        vo.setMemberId(o.getMemberId());
        vo.setOrderType(o.getOrderType());
        vo.setStatus(o.getStatus());
        vo.setStatusText(OrderStatus.textOf(o.getStatus()));
        vo.setGoodsAmount(o.getGoodsAmount());
        vo.setFreightAmount(o.getFreightAmount());
        vo.setCouponAmount(o.getCouponAmount());
        vo.setDiscountAmount(o.getDiscountAmount());
        vo.setAdjustAmount(o.getAdjustAmount());
        vo.setPayAmount(o.getPayAmount());
        vo.setPayType(o.getPayType());
        vo.setPayTime(o.getPayTime());
        vo.setPayExpireAt(o.getPayExpireAt());
        vo.setReceiverName(o.getReceiverName());
        vo.setReceiverMobile(o.getReceiverMobile());
        vo.setReceiverProvince(o.getReceiverProvince());
        vo.setReceiverCity(o.getReceiverCity());
        vo.setReceiverDistrict(o.getReceiverDistrict());
        vo.setReceiverDetail(o.getReceiverDetail());
        vo.setRemark(o.getRemark());
        vo.setAdminRemark(o.getAdminRemark());
        vo.setDeliveryCompany(o.getDeliveryCompany());
        vo.setDeliveryNo(o.getDeliveryNo());
        vo.setDeliveryTime(o.getDeliveryTime());
        vo.setReceiveTime(o.getReceiveTime());
        vo.setCancelTime(o.getCancelTime());
        vo.setCancelReason(o.getCancelReason());
        vo.setReviewed(o.getReviewed());
        vo.setCreatedAt(o.getCreatedAt());
        vo.setItems(items == null ? List.of() : items.stream().map(OrderAssembler::toItemVO).toList());
        return vo;
    }

    public static OrderItemVO toItemVO(OrderItem i) {
        OrderItemVO vo = new OrderItemVO();
        vo.setId(i.getId());
        vo.setGoodsId(i.getGoodsId());
        vo.setSkuId(i.getSkuId());
        vo.setGoodsName(i.getGoodsName());
        vo.setPic(i.getPic());
        vo.setSpecText(i.getSpecText());
        vo.setPrice(i.getPrice());
        vo.setQuantity(i.getQuantity());
        vo.setTotalAmount(i.getTotalAmount());
        vo.setPayAmount(i.getPayAmount());
        vo.setAftersaleStatus(i.getAftersaleStatus());
        vo.setReviewed(i.getReviewed());
        return vo;
    }

    public static OrderLogVO toLogVO(OrderLog l) {
        OrderLogVO vo = new OrderLogVO();
        vo.setId(l.getId());
        vo.setFromStatus(l.getFromStatus());
        vo.setToStatus(l.getToStatus());
        vo.setOperator(l.getOperator());
        vo.setRemark(l.getRemark());
        vo.setCreatedAt(l.getCreatedAt());
        return vo;
    }
}

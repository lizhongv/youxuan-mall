package com.platform.cloud.mall.trade.cart.service;

import com.platform.cloud.mall.trade.cart.vo.CartItemVO;

import java.util.List;

/** 购物车。所有方法以 memberId 为边界，只能操作自己的购物车。 */
public interface CartService {

    List<CartItemVO> list(Long memberId);

    /** 加购，已存在则累加；返回购物车项 ID */
    Long add(Long memberId, Long skuId, int quantity);

    void updateQuantity(Long memberId, Long cartId, int quantity);

    /** ids 为空表示全部 */
    void check(Long memberId, List<Long> ids, boolean checked);

    void delete(Long memberId, List<Long> ids);

    /** 角标：数量合计 */
    long count(Long memberId);
}

package com.platform.cloud.mall.trade.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.goods.api.GoodsApi.SkuSnapshot;
import com.platform.cloud.mall.trade.cart.entity.Cart;
import com.platform.cloud.mall.trade.cart.mapper.CartMapper;
import com.platform.cloud.mall.trade.cart.service.CartService;
import com.platform.cloud.mall.trade.cart.vo.CartItemVO;
import com.platform.cloud.mall.trade.common.TradeDefaults;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 购物车。价格与库存不落库，每次展示从商品领域实时取，保证用户看到的是当前价。
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /** 每个会员最多 100 种商品：足够正常使用，防止被脚本塞爆 */
    static final int MAX_CART_ITEMS = 100;

    private final CartMapper cartMapper;
    private final GoodsApi goodsApi;

    @Override
    public List<CartItemVO> list(Long memberId) {
        List<Cart> carts = cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .orderByDesc(Cart::getUpdatedAt).orderByDesc(Cart::getId));
        if (carts.isEmpty()) {
            return List.of();
        }
        Set<Long> skuIds = carts.stream().map(Cart::getSkuId).collect(Collectors.toSet());
        Map<Long, SkuSnapshot> skus = goodsApi.getSkus(skuIds);
        return carts.stream().map(c -> toVO(c, skus.get(c.getSkuId()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(Long memberId, Long skuId, int quantity) {
        SkuSnapshot sku = requireOnSale(skuId);
        Cart exist = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .eq(Cart::getSkuId, skuId));
        int target = (exist == null ? 0 : exist.getQuantity()) + quantity;
        checkQuantity(sku, target);
        if (exist != null) {
            cartMapper.update(null, Wrappers.<Cart>lambdaUpdate()
                    .eq(Cart::getId, exist.getId())
                    .eq(Cart::getMemberId, memberId)
                    .set(Cart::getQuantity, target)
                    .set(Cart::getChecked, 1));
            return exist.getId();
        }
        long count = cartMapper.selectCount(Wrappers.<Cart>lambdaQuery().eq(Cart::getMemberId, memberId));
        if (count >= MAX_CART_ITEMS) {
            throw new BusinessException("购物车已满，请先清理");
        }
        Cart cart = new Cart();
        cart.setMemberId(memberId);
        cart.setGoodsId(sku.goodsId());
        cart.setSkuId(skuId);
        cart.setQuantity(target);
        cart.setChecked(1);
        cartMapper.insert(cart);
        return cart.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateQuantity(Long memberId, Long cartId, int quantity) {
        Cart cart = cartMapper.selectOne(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getId, cartId)
                .eq(Cart::getMemberId, memberId));
        if (cart == null) {
            throw new BusinessException("购物车商品不存在");
        }
        SkuSnapshot sku = requireOnSale(cart.getSkuId());
        checkQuantity(sku, quantity);
        cartMapper.update(null, Wrappers.<Cart>lambdaUpdate()
                .eq(Cart::getId, cartId)
                .eq(Cart::getMemberId, memberId)
                .set(Cart::getQuantity, quantity));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void check(Long memberId, List<Long> ids, boolean checked) {
        LambdaUpdateWrapper<Cart> w = Wrappers.<Cart>lambdaUpdate()
                .eq(Cart::getMemberId, memberId)
                .in(!CollectionUtils.isEmpty(ids), Cart::getId, ids)
                .set(Cart::getChecked, checked ? 1 : 0);
        cartMapper.update(null, w);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long memberId, List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        cartMapper.delete(Wrappers.<Cart>lambdaQuery()
                .eq(Cart::getMemberId, memberId)
                .in(Cart::getId, ids));
    }

    @Override
    public long count(Long memberId) {
        return cartMapper.selectList(Wrappers.<Cart>lambdaQuery()
                        .select(Cart::getQuantity)
                        .eq(Cart::getMemberId, memberId))
                .stream().mapToLong(c -> c.getQuantity() == null ? 0 : c.getQuantity()).sum();
    }

    private SkuSnapshot requireOnSale(Long skuId) {
        SkuSnapshot sku = goodsApi.getSkus(List.of(skuId)).get(skuId);
        if (sku == null || !sku.onSale()) {
            throw new BusinessException("商品不存在或已下架");
        }
        return sku;
    }

    private static void checkQuantity(SkuSnapshot sku, int quantity) {
        if (quantity < 1) {
            throw new BusinessException("数量至少为 1");
        }
        if (quantity > TradeDefaults.MAX_QUANTITY) {
            throw new BusinessException("单个商品最多购买 " + TradeDefaults.MAX_QUANTITY + " 件");
        }
        if (sku.stock() == null || quantity > sku.stock()) {
            throw new BusinessException("库存不足，当前库存 " + (sku.stock() == null ? 0 : sku.stock()) + " 件");
        }
    }

    private static CartItemVO toVO(Cart c, SkuSnapshot s) {
        CartItemVO vo = new CartItemVO();
        vo.setId(c.getId());
        vo.setGoodsId(c.getGoodsId());
        vo.setSkuId(c.getSkuId());
        vo.setQuantity(c.getQuantity());
        vo.setChecked(c.getChecked() != null && c.getChecked() == 1);
        if (s == null) {
            // SKU 被删除：保留购物车项让用户自己清理，但标记为失效
            vo.setGoodsName("商品已失效");
            vo.setPic("");
            vo.setSpecText("");
            vo.setStock(0);
            vo.setValid(false);
            return vo;
        }
        vo.setGoodsName(s.goodsName());
        vo.setPic(s.pic());
        vo.setSpecText(s.specText());
        vo.setPrice(s.price());
        vo.setStock(s.stock());
        vo.setValid(s.onSale() && s.stock() != null && s.stock() > 0);
        return vo;
    }
}

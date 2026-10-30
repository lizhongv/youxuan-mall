package com.platform.cloud.mall.trade.cart.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.cart.dto.CartAddRequest;
import com.platform.cloud.mall.trade.cart.dto.CartCheckRequest;
import com.platform.cloud.mall.trade.cart.dto.CartDeleteRequest;
import com.platform.cloud.mall.trade.cart.dto.CartUpdateRequest;
import com.platform.cloud.mall.trade.cart.service.CartService;
import com.platform.cloud.mall.trade.cart.vo.CartItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "商城-购物车")
@Validated
@RestController
@RequestMapping("/app/cart")
@RequiredArgsConstructor
public class AppCartController {

    private final CartService cartService;

    @Operation(summary = "购物车列表")
    @GetMapping
    public R<List<CartItemVO>> list() {
        return R.ok(cartService.list(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "加入购物车（已存在则累加）")
    @PostMapping
    public R<Long> add(@Valid @RequestBody CartAddRequest req) {
        return R.ok(cartService.add(SecurityUtils.currentUserId(), req.getSkuId(), req.getQuantity()));
    }

    @Operation(summary = "勾选 / 取消勾选（ids 为空表示全部）")
    @PutMapping("/check")
    public R<Void> check(@Valid @RequestBody CartCheckRequest req) {
        cartService.check(SecurityUtils.currentUserId(), req.getIds(), req.getChecked());
        return R.ok();
    }

    @Operation(summary = "修改数量")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody CartUpdateRequest req) {
        cartService.updateQuantity(SecurityUtils.currentUserId(), id, req.getQuantity());
        return R.ok();
    }

    @Operation(summary = "删除购物车项")
    @DeleteMapping
    public R<Void> delete(@Valid @RequestBody CartDeleteRequest req) {
        cartService.delete(SecurityUtils.currentUserId(), req.getIds());
        return R.ok();
    }

    @Operation(summary = "购物车商品数量合计")
    @GetMapping("/count")
    public R<Long> count() {
        return R.ok(cartService.count(SecurityUtils.currentUserId()));
    }
}

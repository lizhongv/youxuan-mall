package com.platform.cloud.mall.marketing.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.marketing.service.CouponService;
import com.platform.cloud.mall.marketing.vo.MyCouponVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城端：我的优惠券。会员 ID 一律取自令牌，不信任前端传入。
 */
@Tag(name = "商城端-优惠券")
@RestController
@RequestMapping("/app/coupon")
@RequiredArgsConstructor
public class AppCouponController {

    private final CouponService couponService;

    @Operation(summary = "领取优惠券，返回会员券 ID")
    @PostMapping("/{couponId}/receive")
    public R<Long> receive(@PathVariable Long couponId) {
        return R.ok(couponService.receive(couponId, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "我的优惠券")
    @GetMapping("/mine")
    public R<List<MyCouponVO>> mine(@RequestParam(required = false) String status) {
        return R.ok(couponService.mine(SecurityUtils.currentUserId(), status));
    }
}

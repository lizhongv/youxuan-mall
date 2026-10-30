package com.platform.cloud.mall.marketing.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.marketing.service.CouponService;
import com.platform.cloud.mall.marketing.service.NoticeService;
import com.platform.cloud.mall.marketing.service.SeckillService;
import com.platform.cloud.mall.marketing.vo.AvailableCouponVO;
import com.platform.cloud.mall.marketing.vo.NoticeVO;
import com.platform.cloud.mall.marketing.vo.SeckillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城端公开营销接口（无需登录）。
 */
@Tag(name = "商城端-公开营销")
@RestController
@RequestMapping("/app/public")
@RequiredArgsConstructor
public class PublicMarketingController {

    private final CouponService couponService;
    private final SeckillService seckillService;
    private final NoticeService noticeService;

    /** 已登录会员额外返回每张券的已领数量 */
    @Operation(summary = "可领取的优惠券")
    @GetMapping("/coupon/available")
    public R<List<AvailableCouponVO>> availableCoupons() {
        Long memberId = SecurityUtils.isMember() ? SecurityUtils.currentUserIdOrNull() : null;
        return R.ok(couponService.available(memberId));
    }

    @Operation(summary = "秒杀活动列表（进行中及即将开始）")
    @GetMapping("/seckill/list")
    public R<List<SeckillVO>> seckillList() {
        return R.ok(seckillService.listPublic());
    }

    @Operation(summary = "公告详情")
    @GetMapping("/notice/{id}")
    public R<NoticeVO> notice(@PathVariable Long id) {
        return R.ok(noticeService.getPublished(id));
    }
}

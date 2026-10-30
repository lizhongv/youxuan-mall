package com.platform.cloud.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.goods.api.GoodsApi.GoodsLine;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.marketing.api.CouponApi;
import com.platform.cloud.mall.marketing.entity.Coupon;
import com.platform.cloud.mall.marketing.entity.MemberCoupon;
import com.platform.cloud.mall.marketing.mapper.CouponMapper;
import com.platform.cloud.mall.marketing.mapper.MemberCouponMapper;
import com.platform.cloud.mall.marketing.support.CouponCalculator;
import com.platform.cloud.mall.marketing.support.MemberCouponStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 优惠券对交易领域的能力：结算可用券、计算、核销、退还。
 */
@Service
@RequiredArgsConstructor
public class CouponApiImpl implements CouponApi {

    private final CouponMapper couponMapper;
    private final MemberCouponMapper memberCouponMapper;
    private final CategoryService categoryService;

    @Override
    public List<CouponOption> listForCheckout(Long memberId, List<GoodsLine> lines) {
        LocalDateTime now = LocalDateTime.now();
        List<MemberCoupon> owned = memberCouponMapper.selectList(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getStatus, MemberCouponStatus.UNUSED.name())
                .gt(MemberCoupon::getEndAt, now));
        if (owned.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Coupon> templates = couponMapper.selectBatchIds(
                        owned.stream().map(MemberCoupon::getCouponId).distinct().toList())
                .stream().collect(Collectors.toMap(Coupon::getId, Function.identity()));
        Map<Long, Long> parents = categoryParents(lines);
        List<CouponOption> options = new ArrayList<>();
        for (MemberCoupon mc : owned) {
            Coupon c = templates.get(mc.getCouponId());
            if (c == null) {
                continue;
            }
            boolean usable;
            BigDecimal off;
            String reason;
            if (mc.getStartAt().isAfter(now)) {
                usable = false;
                off = BigDecimal.ZERO.setScale(2);
                reason = "未到使用时间";
            } else {
                CouponCalculator.Result r = CouponCalculator.evaluate(c, lines, parents);
                usable = r.usable();
                off = r.discountAmount();
                reason = r.reason();
            }
            options.add(new CouponOption(mc.getId(), c.getName(), c.getType(), c.getThreshold(), c.getAmount(),
                    c.getDiscount(), mc.getEndAt(), usable, off, reason));
        }
        // 可用在前、优惠大在前；优惠相同时快过期的在前，帮用户先用掉
        options.sort(Comparator.comparing(CouponOption::usable).reversed()
                .thenComparing(CouponOption::discountAmount, Comparator.reverseOrder())
                .thenComparing(CouponOption::endAt));
        return options;
    }

    @Override
    public BigDecimal calculate(Long memberId, Long memberCouponId, List<GoodsLine> lines) {
        LocalDateTime now = LocalDateTime.now();
        MemberCoupon mc = memberCouponMapper.selectById(memberCouponId);
        if (mc == null || !Objects.equals(mc.getMemberId(), memberId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (!MemberCouponStatus.UNUSED.name().equals(mc.getStatus())) {
            throw new BusinessException("优惠券已使用或已过期");
        }
        if (mc.getStartAt().isAfter(now)) {
            throw new BusinessException("优惠券未到使用时间");
        }
        if (!mc.getEndAt().isAfter(now)) {
            throw new BusinessException("优惠券已过期");
        }
        Coupon c = couponMapper.selectById(mc.getCouponId());
        if (c == null) {
            throw new BusinessException("优惠券不存在");
        }
        CouponCalculator.Result r = CouponCalculator.evaluate(c, lines, categoryParents(lines));
        if (!r.usable()) {
            throw new BusinessException("优惠券不可用：" + r.reason());
        }
        return r.discountAmount();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void use(Long memberId, Long memberCouponId, Long orderId) {
        MemberCoupon mc = memberCouponMapper.selectById(memberCouponId);
        if (mc == null || !Objects.equals(mc.getMemberId(), memberId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (memberCouponMapper.use(memberCouponId, memberId, orderId, LocalDateTime.now()) == 0) {
            throw new BusinessException("优惠券已使用或已过期");
        }
        couponMapper.increaseUsed(mc.getCouponId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void giveBack(Long memberCouponId) {
        MemberCoupon mc = memberCouponMapper.selectById(memberCouponId);
        if (mc == null) {
            return;
        }
        // 条件更新保证幂等：重复退还（如关单任务与用户取消并发）只有一次生效
        if (memberCouponMapper.giveBack(memberCouponId, LocalDateTime.now()) > 0) {
            couponMapper.decreaseUsed(mc.getCouponId());
        }
    }

    @Override
    public long countUsable(Long memberId) {
        return memberCouponMapper.selectCount(new LambdaQueryWrapper<MemberCoupon>()
                .eq(MemberCoupon::getMemberId, memberId)
                .eq(MemberCoupon::getStatus, MemberCouponStatus.UNUSED.name())
                .gt(MemberCoupon::getEndAt, LocalDateTime.now()));
    }

    private Map<Long, Long> categoryParents(List<GoodsLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return Map.of();
        }
        return categoryService.parentIds(lines.stream().map(GoodsLine::categoryId).filter(Objects::nonNull).toList());
    }
}

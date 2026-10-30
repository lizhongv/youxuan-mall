package com.platform.cloud.mall.marketing.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.marketing.dto.CouponRequest;
import com.platform.cloud.mall.marketing.entity.Coupon;
import com.platform.cloud.mall.marketing.entity.MemberCoupon;
import com.platform.cloud.mall.marketing.mapper.CouponMapper;
import com.platform.cloud.mall.marketing.mapper.MemberCouponMapper;
import com.platform.cloud.mall.member.api.MemberApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 优惠券领取校验、有效期计算、模板表单规整。
 */
@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    private static final long COUPON = 1L;
    private static final long MEMBER = 7L;

    @Mock
    private CouponMapper couponMapper;
    @Mock
    private MemberCouponMapper memberCouponMapper;
    @Mock
    private MemberApi memberApi;
    @InjectMocks
    private CouponServiceImpl service;

    private static Coupon template(String validType) {
        LocalDateTime now = LocalDateTime.now();
        Coupon c = new Coupon();
        c.setId(COUPON);
        c.setType("FULL_REDUCTION");
        c.setStatus(1);
        c.setTotal(100);
        c.setIssued(0);
        c.setPerLimit(1);
        c.setValidType(validType);
        c.setStartAt(now.minusDays(1));
        c.setEndAt(now.plusDays(10));
        c.setValidDays(7);
        return c;
    }

    @Test
    void receiveSuccessDaysValidity() {
        when(couponMapper.selectById(COUPON)).thenReturn(template("DAYS"));
        when(couponMapper.increaseIssued(COUPON)).thenReturn(1);
        when(memberCouponMapper.countReceivedForUpdate(COUPON, MEMBER)).thenReturn(0L);
        service.receive(COUPON, MEMBER);
        ArgumentCaptor<MemberCoupon> captor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponMapper).insert(captor.capture());
        MemberCoupon mc = captor.getValue();
        assertThat(mc.getStatus()).isEqualTo("UNUSED");
        assertThat(mc.getEndAt()).isEqualTo(mc.getStartAt().plusDays(7));
    }

    @Test
    void receiveFixedUsesTemplatePeriod() {
        Coupon c = template("FIXED");
        when(couponMapper.selectById(COUPON)).thenReturn(c);
        when(couponMapper.increaseIssued(COUPON)).thenReturn(1);
        service.receive(COUPON, MEMBER);
        ArgumentCaptor<MemberCoupon> captor = ArgumentCaptor.forClass(MemberCoupon.class);
        verify(memberCouponMapper).insert(captor.capture());
        assertThat(captor.getValue().getStartAt()).isEqualTo(c.getStartAt());
        assertThat(captor.getValue().getEndAt()).isEqualTo(c.getEndAt());
    }

    @Test
    void receiveOverPerLimit() {
        when(couponMapper.selectById(COUPON)).thenReturn(template("DAYS"));
        when(couponMapper.increaseIssued(COUPON)).thenReturn(1);
        when(memberCouponMapper.countReceivedForUpdate(COUPON, MEMBER)).thenReturn(1L);
        assertThatThrownBy(() -> service.receive(COUPON, MEMBER)).hasMessageContaining("每人限领 1 张");
        verify(memberCouponMapper, never()).insert(any(MemberCoupon.class));
    }

    @Test
    void receiveConcurrentSoldOut() {
        when(couponMapper.selectById(COUPON)).thenReturn(template("DAYS"));
        when(couponMapper.increaseIssued(COUPON)).thenReturn(0);
        assertThatThrownBy(() -> service.receive(COUPON, MEMBER)).hasMessage("优惠券已领完");
    }

    @Test
    void checkReceivableRules() {
        LocalDateTime now = LocalDateTime.now();
        assertThatThrownBy(() -> CouponServiceImpl.checkReceivable(null, now)).hasMessage("优惠券不存在");

        Coupon stopped = template("DAYS");
        stopped.setStatus(0);
        assertThatThrownBy(() -> CouponServiceImpl.checkReceivable(stopped, now)).hasMessage("优惠券已停止发放");

        Coupon expired = template("FIXED");
        expired.setEndAt(now);
        assertThatThrownBy(() -> CouponServiceImpl.checkReceivable(expired, now)).hasMessage("优惠券已过期");

        Coupon full = template("DAYS");
        full.setIssued(100);
        assertThatThrownBy(() -> CouponServiceImpl.checkReceivable(full, now)).hasMessage("优惠券已领完");

        Coupon unlimited = template("DAYS");
        unlimited.setTotal(0);
        unlimited.setIssued(100_000);
        CouponServiceImpl.checkReceivable(unlimited, now);
    }

    private static CouponRequest request(String type) {
        CouponRequest r = new CouponRequest();
        r.setName("测试券");
        r.setType(type);
        r.setTotal(0);
        r.setValidType("DAYS");
        r.setValidDays(7);
        return r;
    }

    @Test
    void applyFullReductionValidation() {
        CouponRequest r = request("FULL_REDUCTION");
        r.setThreshold(new BigDecimal("10"));
        r.setAmount(new BigDecimal("10"));
        assertThatThrownBy(() -> CouponServiceImpl.apply(new Coupon(), r)).hasMessageContaining("门槛必须大于");
        r.setThreshold(new BigDecimal("99"));
        Coupon c = new Coupon();
        CouponServiceImpl.apply(c, r);
        assertThat(c.getScope()).isEqualTo("ALL");
        assertThat(c.getScopeIds()).isNull();
        assertThat(c.getDiscount()).isEqualByComparingTo("1");
    }

    @Test
    void applyNoThresholdClearsThreshold() {
        CouponRequest r = request("NO_THRESHOLD");
        r.setThreshold(new BigDecimal("50"));
        r.setAmount(new BigDecimal("5"));
        Coupon c = new Coupon();
        CouponServiceImpl.apply(c, r);
        assertThat(c.getThreshold()).isEqualByComparingTo("0");
    }

    @Test
    void applyDiscountRateRange() {
        CouponRequest r = request("DISCOUNT");
        r.setDiscount(BigDecimal.ONE);
        assertThatThrownBy(() -> CouponServiceImpl.apply(new Coupon(), r)).isInstanceOf(BusinessException.class);
        r.setDiscount(new BigDecimal("0.9"));
        r.setMaxAmount(new BigDecimal("50"));
        r.setAmount(new BigDecimal("8"));
        Coupon c = new Coupon();
        CouponServiceImpl.apply(c, r);
        assertThat(c.getAmount()).isEqualByComparingTo("0");
        assertThat(c.getMaxAmount()).isEqualByComparingTo("50");
    }

    @Test
    void applyScopeRequiresIds() {
        CouponRequest r = request("NO_THRESHOLD");
        r.setAmount(new BigDecimal("5"));
        r.setScope("CATEGORY");
        assertThatThrownBy(() -> CouponServiceImpl.apply(new Coupon(), r)).hasMessage("请选择适用类目");
        r.setScopeIds(List.of(3L, 3L, 4L));
        Coupon c = new Coupon();
        CouponServiceImpl.apply(c, r);
        assertThat(c.getScopeIds()).containsExactly(3L, 4L);
    }

    @Test
    void applyFixedPeriodValidation() {
        CouponRequest r = request("NO_THRESHOLD");
        r.setAmount(new BigDecimal("5"));
        r.setValidType("FIXED");
        assertThatThrownBy(() -> CouponServiceImpl.apply(new Coupon(), r)).hasMessage("请填写有效期");
        LocalDateTime t = LocalDateTime.of(2026, 1, 1, 0, 0);
        r.setStartAt(t);
        r.setEndAt(t);
        assertThatThrownBy(() -> CouponServiceImpl.apply(new Coupon(), r)).hasMessage("结束时间必须晚于开始时间");
    }

    @Test
    void ruleChangedDetectsCoreFields() {
        CouponRequest r = request("NO_THRESHOLD");
        r.setAmount(new BigDecimal("5"));
        Coupon a = new Coupon();
        CouponServiceImpl.apply(a, r);
        Coupon b = new Coupon();
        r.setName("改个名字");
        r.setTotal(500);
        CouponServiceImpl.apply(b, r);
        assertThat(CouponServiceImpl.ruleChanged(a, b)).isFalse();
        r.setAmount(new BigDecimal("6"));
        CouponServiceImpl.apply(b, r);
        assertThat(CouponServiceImpl.ruleChanged(a, b)).isTrue();
    }

    @Test
    void maskMobile() {
        assertThat(CouponServiceImpl.maskMobile("13812345678")).isEqualTo("138****5678");
        assertThat(CouponServiceImpl.maskMobile("138****5678")).isEqualTo("138****5678");
        assertThat(CouponServiceImpl.maskMobile(null)).isNull();
    }
}

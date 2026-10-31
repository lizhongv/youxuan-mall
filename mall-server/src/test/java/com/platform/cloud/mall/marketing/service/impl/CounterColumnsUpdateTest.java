package com.platform.cloud.mall.marketing.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.mall.goods.api.GoodsApi;
import com.platform.cloud.mall.goods.api.GoodsApi.SkuSnapshot;
import com.platform.cloud.mall.marketing.dto.CouponRequest;
import com.platform.cloud.mall.marketing.dto.SeckillRequest;
import com.platform.cloud.mall.marketing.entity.Coupon;
import com.platform.cloud.mall.marketing.entity.Seckill;
import com.platform.cloud.mall.marketing.mapper.CouponMapper;
import com.platform.cloud.mall.marketing.mapper.MemberCouponMapper;
import com.platform.cloud.mall.marketing.mapper.SeckillMapper;
import com.platform.cloud.mall.marketing.support.SeckillStockCache;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理端保存优惠券 / 秒杀活动时不得回写计数列（issued / used / sold），秒杀改库存按差量同步 Redis。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CounterColumnsUpdateTest {

    @Mock
    CouponMapper couponMapper;
    @Mock
    MemberCouponMapper memberCouponMapper;
    @Mock
    MemberApi memberApi;
    @Mock
    SeckillMapper seckillMapper;
    @Mock
    SeckillStockCache stockCache;
    @Mock
    GoodsApi goodsApi;

    @BeforeAll
    static void init() {
        TableInfoTestSupport.init(Coupon.class, Seckill.class);
    }

    @SuppressWarnings("unchecked")
    private static <T> ArgumentCaptor<LambdaUpdateWrapper<T>> captor() {
        return ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
    }

    // ------------------------------------------------------------------ 优惠券

    private static Coupon coupon() {
        Coupon c = new Coupon();
        c.setId(1L);
        c.setName("满100减10");
        c.setType("FULL_REDUCTION");
        c.setThreshold(new BigDecimal("100.00"));
        c.setAmount(new BigDecimal("10.00"));
        c.setDiscount(BigDecimal.ONE);
        c.setMaxAmount(BigDecimal.ZERO.setScale(2));
        c.setScope("ALL");
        c.setTotal(100);
        c.setIssued(30);
        c.setUsed(12);
        c.setPerLimit(1);
        c.setValidType("DAYS");
        c.setValidDays(7);
        c.setStatus(1);
        c.setVersion(3);
        return c;
    }

    private static CouponRequest couponReq() {
        CouponRequest r = new CouponRequest();
        r.setName("满100减10（改名）");
        r.setType("FULL_REDUCTION");
        r.setThreshold(new BigDecimal("100"));
        r.setAmount(new BigDecimal("10"));
        r.setTotal(200);
        r.setPerLimit(1);
        r.setValidType("DAYS");
        r.setValidDays(7);
        r.setStatus(1);
        return r;
    }

    @Test
    void 保存优惠券只写业务字段_不回写已领已用() {
        CouponServiceImpl service = new CouponServiceImpl(couponMapper, memberCouponMapper, memberApi);
        when(couponMapper.selectById(1L)).thenReturn(coupon());
        when(couponMapper.update(isNull(), any())).thenReturn(1);

        service.update(1L, couponReq());

        ArgumentCaptor<LambdaUpdateWrapper<Coupon>> w = captor();
        verify(couponMapper).update(isNull(), w.capture());
        assertThat(w.getValue().getSqlSet()).contains("name", "total", "version")
                .doesNotContain("issued=").doesNotContain("used=");
        // version 条件 + 发行量不小于当前已领（数据库侧兜底）
        assertThat(w.getValue().getSqlSegment()).contains("version").contains("issued <=");
        verify(couponMapper, never()).updateById(any(Coupon.class));
    }

    @Test
    void 优惠券并发修改时提示刷新() {
        CouponServiceImpl service = new CouponServiceImpl(couponMapper, memberCouponMapper, memberApi);
        when(couponMapper.selectById(1L)).thenReturn(coupon());
        when(couponMapper.update(isNull(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.update(1L, couponReq())).hasMessageContaining("请刷新后重试");
    }

    @Test
    void 优惠券启停只改状态列() {
        CouponServiceImpl service = new CouponServiceImpl(couponMapper, memberCouponMapper, memberApi);
        when(couponMapper.selectById(1L)).thenReturn(coupon());
        when(couponMapper.update(isNull(), any())).thenReturn(1);

        service.updateStatus(1L, 0);

        ArgumentCaptor<LambdaUpdateWrapper<Coupon>> w = captor();
        verify(couponMapper).update(isNull(), w.capture());
        assertThat(w.getValue().getSqlSet()).startsWith("status=").doesNotContain(",");
        verify(couponMapper, never()).updateById(any(Coupon.class));
    }

    // ------------------------------------------------------------------ 秒杀

    private static Seckill seckill() {
        Seckill s = new Seckill();
        s.setId(9L);
        s.setTitle("秒杀");
        s.setGoodsId(10L);
        s.setSkuId(20L);
        s.setSeckillPrice(new BigDecimal("1.00"));
        s.setStock(10);
        s.setSold(4);
        s.setLimitPerMember(1);
        s.setStartAt(LocalDateTime.now().minusHours(1));
        s.setEndAt(LocalDateTime.now().plusHours(1));
        s.setStatus(1);
        s.setVersion(2);
        return s;
    }

    private static SeckillRequest seckillReq(int stock) {
        SeckillRequest r = new SeckillRequest();
        r.setTitle("秒杀");
        r.setGoodsId(10L);
        r.setSkuId(20L);
        r.setSeckillPrice(new BigDecimal("1.00"));
        r.setStock(stock);
        r.setLimitPerMember(1);
        r.setStartAt(LocalDateTime.now().minusHours(1));
        r.setEndAt(LocalDateTime.now().plusHours(1));
        r.setStatus(1);
        return r;
    }

    private SeckillServiceImpl seckillService() {
        when(goodsApi.getSkus(any())).thenReturn(Map.of(20L,
                new SkuSnapshot(20L, 10L, 1L, "商品", "", "", new BigDecimal("99"), 100, true)));
        when(seckillMapper.exists(any())).thenReturn(false);
        return new SeckillServiceImpl(seckillMapper, stockCache, goodsApi);
    }

    @Test
    void 保存秒杀活动不回写sold_Redis按库存差量调整() {
        SeckillServiceImpl service = seckillService();
        when(seckillMapper.selectById(9L)).thenReturn(seckill());
        when(seckillMapper.update(isNull(), any())).thenReturn(1);
        when(stockCache.adjust(eq(9L), anyInt(), any())).thenReturn(true);

        // 无事务时 afterCommit 回调立即执行
        service.update(9L, seckillReq(15));

        ArgumentCaptor<LambdaUpdateWrapper<Seckill>> w = captor();
        verify(seckillMapper).update(isNull(), w.capture());
        assertThat(w.getValue().getSqlSet()).contains("stock=").doesNotContain("sold=");
        assertThat(w.getValue().getSqlSegment()).contains("version").contains("sold <=");
        verify(seckillMapper, never()).updateById(any(Seckill.class));
        // 10 → 15：Redis +5，不用读取时的 sold 覆盖写
        verify(stockCache).adjust(eq(9L), eq(5), any());
        verify(stockCache, never()).warm(anyLong(), anyInt(), any());
    }

    @Test
    void 秒杀Redis键不存在时以提交后数据库最新sold懒加载() {
        SeckillServiceImpl service = seckillService();
        Seckill fresh = seckill();
        fresh.setStock(15);
        fresh.setSold(6);
        when(seckillMapper.selectById(9L)).thenReturn(seckill(), fresh);
        when(seckillMapper.update(isNull(), any())).thenReturn(1);
        when(stockCache.adjust(eq(9L), anyInt(), any())).thenReturn(false);

        service.update(9L, seckillReq(15));

        verify(stockCache).warmIfAbsent(eq(9L), eq(9), any());
    }

    @Test
    void 秒杀并发编辑失败() {
        SeckillServiceImpl service = seckillService();
        when(seckillMapper.selectById(9L)).thenReturn(seckill());
        when(seckillMapper.update(isNull(), any())).thenReturn(0);
        assertThatThrownBy(() -> service.update(9L, seckillReq(15))).hasMessageContaining("请刷新后重试");
        verify(stockCache, never()).adjust(anyLong(), anyInt(), any());
    }

    @Test
    void 启动预热不覆盖已有Redis计数() {
        SeckillServiceImpl service = seckillService();
        when(seckillMapper.selectList(any())).thenReturn(List.of(seckill()));

        service.warmUpAll();

        verify(stockCache).warmIfAbsent(eq(9L), eq(6), any());
        verify(stockCache, never()).warm(anyLong(), anyInt(), any());
    }
}

package com.platform.cloud.mall.trade.logistics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import com.platform.cloud.mall.system.service.SysDictService;
import com.platform.cloud.mall.system.vo.DictItemVO;
import com.platform.cloud.mall.testsupport.FakeConfigApi;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import com.platform.cloud.mall.trade.logistics.support.ExpressCompanyResolver;
import com.platform.cloud.mall.trade.logistics.support.Kuaidi100Client;
import com.platform.cloud.mall.trade.logistics.support.LogisticsState;
import com.platform.cloud.mall.trade.logistics.vo.LogisticsTraceVO;
import com.platform.cloud.mall.trade.logistics.vo.LogisticsVO;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 物流查询：开通判断、订单归属、缓存策略（在途 30 分钟 / 签收 1 天 / 无结果 10 分钟 / 其他失败不缓存）、顺丰手机号后四位。
 */
class LogisticsServiceImplTest {

    private OrderMapper orderMapper;
    private FakeConfigApi config;
    private Kuaidi100Client kuaidi100;
    private ValueOperations<String, String> ops;
    private LogisticsServiceImpl service;

    @BeforeAll
    static void initMp() {
        TableInfoTestSupport.init(Order.class);
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        config = new FakeConfigApi()
                .put(InfraConfigKeys.LOGISTICS_PROVIDER, "KUAIDI100")
                .put(InfraConfigKeys.LOGISTICS_KUAIDI100_CUSTOMER, "CUST")
                .put(InfraConfigKeys.LOGISTICS_KUAIDI100_KEY, "KEY");
        SysDictService dict = mock(SysDictService.class);
        when(dict.items(ExpressCompanyResolver.DICT_TYPE)).thenReturn(List.of(
                new DictItemVO("顺丰速运", "shunfeng"), new DictItemVO("中通快递", "zhongtong")));
        kuaidi100 = mock(Kuaidi100Client.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new LogisticsServiceImpl(orderMapper, config, new ExpressCompanyResolver(dict), kuaidi100, redis);
    }

    @Test
    void notEnabledReturnsEmptyTracesWithHint() {
        config.put(InfraConfigKeys.LOGISTICS_PROVIDER, "NONE");
        LogisticsVO vo = service.query(order("中通快递", "ZT1"));
        assertThat(vo.getTraces()).isEmpty();
        assertThat(vo.getMessage()).isEqualTo(LogisticsServiceImpl.MSG_NOT_ENABLED);
        assertThat(vo.getCompany()).isEqualTo("zhongtong");
        verify(kuaidi100, never()).query(any(), any(), any(), any(), any());

        // 选了快递100 但没填 key 同样视为未开通
        config.put(InfraConfigKeys.LOGISTICS_PROVIDER, "KUAIDI100").put(InfraConfigKeys.LOGISTICS_KUAIDI100_KEY, "");
        assertThat(service.query(order("中通快递", "ZT1")).getMessage()).isEqualTo(LogisticsServiceImpl.MSG_NOT_ENABLED);
    }

    @Test
    void notDeliveredAndUnsupportedCompany() {
        assertThat(service.query(order(null, null)).getMessage()).isEqualTo(LogisticsServiceImpl.MSG_NOT_DELIVERED);
        LogisticsVO vo = service.query(order("某某快递", "X1"));
        assertThat(vo.getMessage()).isEqualTo(LogisticsServiceImpl.MSG_UNSUPPORTED);
        assertThat(vo.getCompanyName()).isEqualTo("某某快递");
    }

    @Test
    void inTransitResultIsCachedThirtyMinutes() {
        when(kuaidi100.query("CUST", "KEY", "zhongtong", "ZT1", null)).thenReturn(ok(LogisticsState.IN_TRANSIT, false));
        LogisticsVO vo = service.query(order("中通快递", "ZT1"));
        assertThat(vo.getState()).isEqualTo("IN_TRANSIT");
        assertThat(vo.getStateText()).isEqualTo("运输中");
        assertThat(vo.getTraces()).hasSize(1);
        assertThat(vo.getMessage()).isNull();
        verify(ops).set(eq("mall:logistics:zhongtong:ZT1"), anyString(), eq(LogisticsServiceImpl.TTL_IN_TRANSIT));
    }

    @Test
    void signedResultIsCachedOneDayAndShunfengSendsLastFourDigits() {
        when(kuaidi100.query("CUST", "KEY", "shunfeng", "SF1", "5678")).thenReturn(ok(LogisticsState.SIGNED, true));
        LogisticsVO vo = service.query(order("顺丰速运", "SF1"));
        assertThat(vo.getState()).isEqualTo("SIGNED");
        verify(ops).set(eq("mall:logistics:shunfeng:SF1"), anyString(), eq(LogisticsServiceImpl.TTL_SIGNED));
    }

    @Test
    void cacheHitSkipsRemoteQuery() throws Exception {
        LogisticsVO cached = new LogisticsVO();
        cached.setCompany("zhongtong");
        cached.setState("DELIVERING");
        cached.setTraces(List.of(new LogisticsTraceVO("2026-10-31 10:00:00", "派送中", "杭州")));
        when(ops.get("mall:logistics:zhongtong:ZT1")).thenReturn(new ObjectMapper().writeValueAsString(cached));

        LogisticsVO vo = service.query(order("中通快递", "ZT1"));
        assertThat(vo.getState()).isEqualTo("DELIVERING");
        assertThat(vo.getTraces()).extracting("location").containsExactly("杭州");
        verify(kuaidi100, never()).query(any(), any(), any(), any(), any());
    }

    @Test
    void noResultCachedShortlyOtherFailuresNotCached() {
        when(kuaidi100.query(any(), any(), eq("zhongtong"), eq("ZT1"), isNull()))
                .thenReturn(new Kuaidi100Client.Result(false, LogisticsState.NONE, false, List.of(), "500", "无结果"));
        assertThat(service.query(order("中通快递", "ZT1")).getMessage()).isEqualTo(LogisticsServiceImpl.MSG_NO_RESULT);
        verify(ops).set(eq("mall:logistics:zhongtong:ZT1"), anyString(), eq(LogisticsServiceImpl.TTL_NO_RESULT));

        when(kuaidi100.query(any(), any(), eq("zhongtong"), eq("ZT2"), isNull()))
                .thenReturn(new Kuaidi100Client.Result(false, LogisticsState.NONE, false, List.of(), "503", "签名错误"));
        LogisticsVO vo = service.query(order("中通快递", "ZT2"));
        assertThat(vo.getMessage()).isEqualTo(LogisticsServiceImpl.MSG_FAILED);
        assertThat(vo.getTraces()).isEmpty();
        verify(ops, never()).set(eq("mall:logistics:zhongtong:ZT2"), anyString(), any());
    }

    @Test
    void redisFailureDoesNotBreakQuery() {
        when(ops.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(kuaidi100.query(any(), any(), any(), any(), any())).thenReturn(ok(LogisticsState.COLLECTED, false));
        assertThat(service.query(order("中通快递", "ZT1")).getState()).isEqualTo("COLLECTED");
    }

    @Test
    void memberCannotQueryOthersOrder() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        assertThatThrownBy(() -> service.memberLogistics(1L, 99L))
                .isInstanceOf(BusinessException.class).hasMessage("订单不存在");
        when(orderMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.adminLogistics(99L)).isInstanceOf(BusinessException.class);
    }

    @Test
    void lastFourDigits() {
        assertThat(LogisticsServiceImpl.lastFour("138-0000-5678")).isEqualTo("5678");
        assertThat(LogisticsServiceImpl.lastFour("12")).isNull();
        assertThat(LogisticsServiceImpl.lastFour(null)).isNull();
    }

    private static Kuaidi100Client.Result ok(LogisticsState state, boolean signed) {
        return new Kuaidi100Client.Result(true, state, signed,
                List.of(new LogisticsTraceVO("2026-10-31 10:00:00", "快件到达", "杭州")), null, null);
    }

    private static Order order(String company, String no) {
        Order o = new Order();
        o.setId(1L);
        o.setMemberId(1L);
        o.setDeliveryCompany(company);
        o.setDeliveryNo(no);
        o.setReceiverMobile("13800005678");
        return o;
    }
}

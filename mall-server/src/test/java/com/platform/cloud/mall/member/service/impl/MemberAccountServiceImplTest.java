package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.entity.MbrAccountLog;
import com.platform.cloud.mall.member.entity.MbrLevel;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrAccountLogMapper;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 余额 / 积分扣减与成长值升级。
 */
class MemberAccountServiceImplTest {

    private MbrMemberMapper memberMapper;
    private MbrAccountLogMapper logMapper;
    private MemberLevelService levelService;
    private MemberAccountServiceImpl service;

    @BeforeAll
    static void initTables() {
        TableInfoTestSupport.init(MbrMember.class, MbrAccountLog.class);
    }

    @BeforeEach
    void setUp() {
        memberMapper = mock(MbrMemberMapper.class);
        logMapper = mock(MbrAccountLogMapper.class);
        levelService = mock(MemberLevelService.class);
        service = new MemberAccountServiceImpl(memberMapper, logMapper, levelService);
    }

    @Test
    void deductBalanceWhenInsufficientThrowsAndWritesNoLog() {
        when(memberMapper.changeBalance(eq(1L), any())).thenReturn(0);
        when(memberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.changeBalance(1L, new BigDecimal("-50.00"), "ORDER_PAY", "NO1", ""))
                .isInstanceOf(BusinessException.class)
                .hasMessage("余额不足");
        verify(logMapper, never()).insert(any(MbrAccountLog.class));
    }

    @Test
    void deductBalanceUsesConditionalUpdateAndLogsAfterValue() {
        when(memberMapper.changeBalance(1L, new BigDecimal("-30.00"))).thenReturn(1);
        when(memberMapper.selectById(1L)).thenReturn(member(new BigDecimal("70.00"), 0, 0, null));

        service.changeBalance(1L, new BigDecimal("-30"), "ORDER_PAY", "NO1", "支付");

        ArgumentCaptor<MbrAccountLog> captor = ArgumentCaptor.forClass(MbrAccountLog.class);
        verify(logMapper).insert(captor.capture());
        MbrAccountLog log = captor.getValue();
        assertThat(log.getAccountType()).isEqualTo("BALANCE");
        assertThat(log.getChangeValue()).isEqualByComparingTo("-30.00");
        assertThat(log.getAfterValue()).isEqualByComparingTo("70.00");
        assertThat(log.getBizType()).isEqualTo("ORDER_PAY");
    }

    @Test
    void balanceDeltaIsRoundedToCents() {
        when(memberMapper.changeBalance(1L, new BigDecimal("10.01"))).thenReturn(1);
        when(memberMapper.selectById(1L)).thenReturn(member(new BigDecimal("10.01"), 0, 0, null));

        service.changeBalance(1L, new BigDecimal("10.005"), "RECHARGE", "", "");

        verify(memberMapper).changeBalance(1L, new BigDecimal("10.01"));
    }

    @Test
    void unknownMemberReportsNotFoundInsteadOfInsufficient() {
        when(memberMapper.changeBalance(eq(9L), any())).thenReturn(0);
        when(memberMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> service.changeBalance(9L, new BigDecimal("-1"), "ORDER_PAY", "", ""))
                .hasMessage("会员不存在");
    }

    @Test
    void deductPointsWhenInsufficientThrows() {
        when(memberMapper.changePoints(1L, -100)).thenReturn(0);
        when(memberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.changePoints(1L, -100, "ORDER_PAY", "", ""))
                .hasMessage("积分不足");
    }

    @Test
    void zeroDeltaIsNoop() {
        service.changeBalance(1L, BigDecimal.ZERO, "X", "", "");
        service.changePoints(1L, 0, "X", "", "");
        verify(memberMapper, never()).changeBalance(anyLong(), any());
        verify(memberMapper, never()).changePoints(anyLong(), anyInt());
    }

    @Test
    void addGrowthUpgradesWhenThresholdReached() {
        MbrLevel normal = level(1L, 0);
        MbrLevel silver = level(2L, 500);
        when(memberMapper.addGrowth(1L, 120)).thenReturn(1);
        when(memberMapper.selectById(1L)).thenReturn(member(BigDecimal.ZERO, 0, 520, 1L));
        when(levelService.matchLevel(520)).thenReturn(silver);
        when(levelService.get(1L)).thenReturn(normal);

        service.addGrowth(1L, 120, "ORDER_REWARD", "NO1", "");

        verify(memberMapper).update(isNull(), any());
        ArgumentCaptor<MbrAccountLog> captor = ArgumentCaptor.forClass(MbrAccountLog.class);
        verify(logMapper).insert(captor.capture());
        assertThat(captor.getValue().getAccountType()).isEqualTo("GROWTH");
        assertThat(captor.getValue().getAfterValue()).isEqualByComparingTo("520");
    }

    @Test
    void addGrowthDoesNotChangeLevelBelowNextThreshold() {
        when(memberMapper.addGrowth(1L, 10)).thenReturn(1);
        when(memberMapper.selectById(1L)).thenReturn(member(BigDecimal.ZERO, 0, 100, 1L));
        when(levelService.matchLevel(100)).thenReturn(level(1L, 0));

        service.addGrowth(1L, 10, "ORDER_REWARD", "", "");

        verify(memberMapper, never()).update(any(), any());
    }

    @Test
    void neverDowngradesManuallyRaisedLevel() {
        // 会员当前是黄金（2000），成长值只有 600，匹配到白银，不应被「降」回白银
        when(memberMapper.addGrowth(1L, 100)).thenReturn(1);
        when(memberMapper.selectById(1L)).thenReturn(member(BigDecimal.ZERO, 0, 600, 3L));
        when(levelService.matchLevel(600)).thenReturn(level(2L, 500));
        when(levelService.get(3L)).thenReturn(level(3L, 2000));

        service.addGrowth(1L, 100, "ORDER_REWARD", "", "");

        verify(memberMapper, never()).update(any(), any());
    }

    @Test
    void nonPositiveGrowthIsIgnored() {
        service.addGrowth(1L, -5, "ORDER_REFUND", "", "");
        verify(memberMapper, never()).addGrowth(anyLong(), anyInt());
    }

    private static MbrMember member(BigDecimal balance, int points, int growth, Long levelId) {
        MbrMember m = new MbrMember();
        m.setId(1L);
        m.setBalance(balance);
        m.setPoints(points);
        m.setGrowth(growth);
        m.setLevelId(levelId);
        return m;
    }

    private static MbrLevel level(Long id, int minGrowth) {
        MbrLevel l = new MbrLevel();
        l.setId(id);
        l.setName("L" + id);
        l.setMinGrowth(minGrowth);
        l.setDiscount(BigDecimal.ONE);
        return l;
    }
}

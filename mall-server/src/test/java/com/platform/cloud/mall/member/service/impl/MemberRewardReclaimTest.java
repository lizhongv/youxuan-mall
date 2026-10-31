package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.entity.MbrAccountLog;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 退款回收订单奖励：积分 / 成长值扣到 0 为止、写流水、不降级。
 */
class MemberRewardReclaimTest {

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

    private static MbrMember account(int points, int growth) {
        MbrMember m = new MbrMember();
        m.setId(1L);
        m.setPoints(points);
        m.setGrowth(growth);
        return m;
    }

    @Test
    void 余额充足时按计划扣回并各写一条流水() {
        when(memberMapper.selectAccountForUpdate(1L)).thenReturn(account(100, 600));

        service.reclaimReward(1L, 30, 15, "ORDER_REWARD_REVOKE", "A1", "退款扣回");

        verify(memberMapper).deductReward(1L, 30, 15);
        ArgumentCaptor<MbrAccountLog> logs = ArgumentCaptor.forClass(MbrAccountLog.class);
        verify(logMapper, times(2)).insert(logs.capture());
        List<MbrAccountLog> l = logs.getAllValues();
        assertThat(l.get(0).getAccountType()).isEqualTo("POINTS");
        assertThat(l.get(0).getChangeValue()).isEqualByComparingTo("-30");
        assertThat(l.get(0).getAfterValue()).isEqualByComparingTo("70");
        assertThat(l.get(1).getAccountType()).isEqualTo("GROWTH");
        assertThat(l.get(1).getAfterValue()).isEqualByComparingTo("585");
        // 等级只升不降：不触碰等级
        verifyNoInteractions(levelService);
    }

    @Test
    void 积分已被花掉时扣到0并在流水注明() {
        when(memberMapper.selectAccountForUpdate(1L)).thenReturn(account(10, 5));

        service.reclaimReward(1L, 30, 15, "ORDER_REWARD_REVOKE", "A1", "退款扣回");

        verify(memberMapper).deductReward(1L, 10, 5);
        ArgumentCaptor<MbrAccountLog> logs = ArgumentCaptor.forClass(MbrAccountLog.class);
        verify(logMapper, times(2)).insert(logs.capture());
        assertThat(logs.getAllValues().get(0).getAfterValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(logs.getAllValues().get(0).getRemark()).contains("积分不足");
    }

    @Test
    void 已经为0时不扣也不写流水() {
        when(memberMapper.selectAccountForUpdate(1L)).thenReturn(account(0, 0));

        service.reclaimReward(1L, 30, 15, "ORDER_REWARD_REVOKE", "A1", "退款扣回");

        verify(memberMapper, never()).deductReward(anyLong(), anyInt(), anyInt());
        verify(logMapper, never()).insert(any(MbrAccountLog.class));
    }

    @Test
    void 计划为0时直接返回() {
        service.reclaimReward(1L, 0, 0, "ORDER_REWARD_REVOKE", "A1", "");
        verifyNoInteractions(memberMapper);
    }

    @Test
    void 会员不存在() {
        assertThatThrownBy(() -> service.reclaimReward(1L, 1, 1, "ORDER_REWARD_REVOKE", "A1", ""))
                .isInstanceOf(BusinessException.class).hasMessage("会员不存在");
    }
}

package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.common.security.RedisCounter;
import com.platform.cloud.mall.member.dto.PayPasswordRequest;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import com.platform.cloud.mall.member.service.SmsCodeService;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.trade.MpTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 余额支付密码：未设置 / 错误计数 / 锁定 / 正确清零 / 弱密码拒绝 / 设置流程。
 */
class PayPasswordServiceImplTest {

    private static final Long MEMBER = 1001L;
    private static final String MOBILE = "13800000000";
    private static final String KEY = "mall:pay-pwd-fail:" + MEMBER;
    /** 测试用低成本 BCrypt，避免拖慢构建 */
    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder(4);

    private MbrMemberMapper memberMapper;
    private SmsCodeService smsCodeService;
    private RedisCounter counter;
    private PayPasswordServiceImpl service;
    private MbrMember member;

    @BeforeAll
    static void initMp() {
        MpTestSupport.init(MbrMember.class);
    }

    @BeforeEach
    void setUp() {
        memberMapper = mock(MbrMemberMapper.class);
        smsCodeService = mock(SmsCodeService.class);
        counter = mock(RedisCounter.class);
        service = new PayPasswordServiceImpl(memberMapper, smsCodeService, ENCODER, counter);
        member = new MbrMember();
        member.setId(MEMBER);
        member.setMobile(MOBILE);
        member.setPayPassword(ENCODER.encode("135790"));
        when(memberMapper.selectById(MEMBER)).thenReturn(member);
    }

    private BusinessException verifyFails(String pwd) {
        return catchThrowableOfType(() -> service.verify(MEMBER, pwd), BusinessException.class);
    }

    // ------------------------------------------------------------------ 校验

    @Test
    void 未设置支付密码返回60010且不计数() {
        member.setPayPassword("");

        BusinessException e = verifyFails("135790");

        assertThat(e.getCode()).isEqualTo(60010);
        assertThat(e.getMessage()).isEqualTo("请先设置支付密码");
        verify(counter, never()).incrementSliding(anyString(), any());
    }

    @Test
    void 密码错误返回60011并提示剩余次数() {
        when(counter.incrementSliding(KEY, Duration.ofMinutes(30))).thenReturn(1L);

        BusinessException e = verifyFails("000000");

        assertThat(e.getCode()).isEqualTo(60011);
        assertThat(e.getMessage()).isEqualTo("支付密码错误，还可尝试 4 次");
        verify(counter, never()).delete(KEY);
    }

    @Test
    void 第4次错误还剩1次() {
        when(counter.get(KEY)).thenReturn(3L);
        when(counter.incrementSliding(KEY, Duration.ofMinutes(30))).thenReturn(4L);

        assertThat(verifyFails("000000").getMessage()).isEqualTo("支付密码错误，还可尝试 1 次");
    }

    @Test
    void 第5次错误即锁定返回60012() {
        when(counter.get(KEY)).thenReturn(4L);
        when(counter.incrementSliding(KEY, Duration.ofMinutes(30))).thenReturn(5L);

        BusinessException e = verifyFails("000000");

        assertThat(e.getCode()).isEqualTo(60012);
        assertThat(e.getMessage()).isEqualTo("支付密码错误次数过多，请 30 分钟后再试");
    }

    @Test
    void 锁定期间即使密码正确也直接拒绝且不再计数() {
        when(counter.get(KEY)).thenReturn(5L);

        BusinessException e = verifyFails("135790");

        assertThat(e.getCode()).isEqualTo(60012);
        verify(counter, never()).incrementSliding(anyString(), any());
        verify(counter, never()).delete(KEY);
    }

    @Test
    void 并发请求抢到超出上限的名额时按锁定处理() {
        when(counter.get(KEY)).thenReturn(4L);
        when(counter.incrementSliding(KEY, Duration.ofMinutes(30))).thenReturn(6L);

        assertThat(verifyFails("135790").getCode()).isEqualTo(60012);
    }

    @Test
    void 密码正确清零错误计数() {
        when(counter.get(KEY)).thenReturn(2L);
        when(counter.incrementSliding(KEY, Duration.ofMinutes(30))).thenReturn(3L);

        assertThatCode(() -> service.verify(MEMBER, "135790")).doesNotThrowAnyException();
        verify(counter).delete(KEY);
    }

    @Test
    void 未传密码按错误处理() {
        when(counter.incrementSliding(KEY, Duration.ofMinutes(30))).thenReturn(1L);

        assertThat(verifyFails(null).getCode()).isEqualTo(60011);
    }

    // ------------------------------------------------------------------ 设置

    private static PayPasswordRequest req(String code, String pwd) {
        PayPasswordRequest r = new PayPasswordRequest();
        r.setCode(code);
        r.setPayPassword(pwd);
        return r;
    }

    @ParameterizedTest
    @ValueSource(strings = {"000000", "111111", "999999", "123456", "234567", "456789", "012345",
            "654321", "987654", "543210"})
    void 弱密码被拒绝且不消费验证码(String pwd) {
        BusinessException e = catchThrowableOfType(() -> service.setPayPassword(MEMBER, req("123456", pwd)),
                BusinessException.class);

        assertThat(e.getMessage()).contains("过于简单");
        verify(smsCodeService, never()).verify(anyString(), any(), anyString());
        verify(memberMapper, never()).update(isNull(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "1234567", "12a456", ""})
    void 非6位数字被拒绝(String pwd) {
        BusinessException e = catchThrowableOfType(() -> PayPasswordServiceImpl.checkStrength(pwd),
                BusinessException.class);
        assertThat(e.getMessage()).isEqualTo("支付密码为 6 位数字");
    }

    @ParameterizedTest
    @ValueSource(strings = {"135790", "112233", "123457", "909090", "012346"})
    void 普通密码通过强度校验(String pwd) {
        assertThatCode(() -> PayPasswordServiceImpl.checkStrength(pwd)).doesNotThrowAnyException();
    }

    @Test
    void 凭验证码设置成功_BCrypt存储并清零错误计数() {
        service.setPayPassword(MEMBER, req("246810", "258036"));

        InOrder order = inOrder(smsCodeService, memberMapper, counter);
        order.verify(smsCodeService).verify(MOBILE, SmsScene.PAY_PASSWORD, "246810");
        order.verify(memberMapper).update(isNull(), any());
        order.verify(counter).delete(KEY);
    }

    @Test
    void 验证码错误时不修改密码() {
        doThrow(new BusinessException("验证码错误")).when(smsCodeService)
                .verify(MOBILE, SmsScene.PAY_PASSWORD, "000001");

        BusinessException e = catchThrowableOfType(() -> service.setPayPassword(MEMBER, req("000001", "258036")),
                BusinessException.class);

        assertThat(e.getMessage()).isEqualTo("验证码错误");
        verify(memberMapper, never()).update(isNull(), any());
        verify(counter, never()).delete(anyString());
    }

    @Test
    void 发送验证码使用当前会员手机号与PAY_PASSWORD场景() {
        service.sendSmsCode(MEMBER, "cid", "abcd");

        verify(smsCodeService).send(eq(MOBILE), eq(SmsScene.PAY_PASSWORD), eq("cid"), eq("abcd"));
    }
}

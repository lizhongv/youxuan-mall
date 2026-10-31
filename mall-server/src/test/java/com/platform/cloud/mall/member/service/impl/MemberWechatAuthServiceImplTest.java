package com.platform.cloud.mall.member.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.dto.WechatBindRequest;
import com.platform.cloud.mall.member.entity.MbrSocial;
import com.platform.cloud.mall.member.mapper.MbrSocialMapper;
import com.platform.cloud.mall.member.service.MemberAuthService;
import com.platform.cloud.mall.member.support.WechatMpClient;
import com.platform.cloud.mall.member.support.WechatMpClient.OAuthResult;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.member.vo.WechatAuthorizeVO;
import com.platform.cloud.mall.member.vo.WechatLoginVO;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import com.platform.cloud.mall.testsupport.TableInfoTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 微信网页授权登录：授权地址与 state、已绑定直接登录、未绑定返回 bindToken、绑定手机号的各种冲突。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MemberWechatAuthServiceImplTest {

    @Mock
    ConfigApi configApi;
    @Mock
    WechatMpClient mpClient;
    @Mock
    MbrSocialMapper socialMapper;
    @Mock
    MemberAuthService authService;
    @Mock
    StringRedisTemplate redis;
    @Mock
    ValueOperations<String, String> ops;
    @InjectMocks
    MemberWechatAuthServiceImpl service;

    MemberTokenVO token = new MemberTokenVO("a", "r", 7200);

    @BeforeAll
    static void initMp() {
        TableInfoTestSupport.init(MbrSocial.class);
    }

    @BeforeEach
    void setUp() {
        when(configApi.getString(ConfigKeys.WECHAT_MP_APP_ID, "")).thenReturn("wx-mp");
        when(configApi.getString(ConfigKeys.WECHAT_MP_APP_SECRET, "")).thenReturn("secret");
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.delete(anyString())).thenReturn(true);
        when(mpClient.exchangeCode("wx-mp", "secret", "code1")).thenReturn(new OAuthResult("openid-1", ""));
        when(authService.loginTrusted(any(), anyString())).thenReturn(token);
    }

    @Test
    void 授权地址_静默授权_state存Redis() {
        WechatAuthorizeVO vo = service.authorizeUrl("https://shop.example.com/#/auth/wechat");

        assertThat(vo.getUrl()).startsWith("https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx-mp")
                .contains("scope=snsapi_base")
                .contains("redirect_uri=https%3A%2F%2Fshop.example.com%2F%23%2Fauth%2Fwechat")
                .contains("state=" + vo.getState())
                .endsWith("#wechat_redirect");
        verify(ops).set(eq(MemberWechatAuthServiceImpl.STATE_PREFIX + vo.getState()), eq("1"), any(Duration.class));
    }

    @Test
    void 授权地址_未配置公众号或回跳地址非法() {
        assertThatThrownBy(() -> service.authorizeUrl("javascript:alert(1)")).hasMessageContaining("回跳地址");
        when(configApi.getString(ConfigKeys.WECHAT_MP_APP_ID, "")).thenReturn("");
        assertThatThrownBy(() -> service.authorizeUrl("https://x")).hasMessageContaining("未开启微信登录");
    }

    @Test
    void 登录_state无效或已用过_拒绝() {
        when(redis.delete(anyString())).thenReturn(false);
        assertThatThrownBy(() -> service.login("code1", "bad")).hasMessageContaining("授权已过期");
        verify(mpClient, never()).exchangeCode(anyString(), anyString(), anyString());
    }

    @Test
    void 登录_已绑定直接签发令牌() {
        MbrSocial s = new MbrSocial();
        s.setMemberId(1001L);
        when(socialMapper.selectOne(any())).thenReturn(s);

        WechatLoginVO vo = service.login("code1", "st");

        assertThat(vo.getNeedBind()).isFalse();
        assertThat(vo.getAccessToken()).isEqualTo("a");
        verify(authService).loginTrusted(eq(1001L), anyString());
    }

    @Test
    void 登录_未绑定返回bindToken_openid不下发() {
        WechatLoginVO vo = service.login("code1", "st");

        assertThat(vo.getNeedBind()).isTrue();
        assertThat(vo.getBindToken()).isNotBlank();
        assertThat(vo.getAccessToken()).isNull();
        verify(ops).set(eq(MemberWechatAuthServiceImpl.BIND_PREFIX + vo.getBindToken()), eq("openid-1|"),
                any(Duration.class));
    }

    private static WechatBindRequest bindReq() {
        WechatBindRequest r = new WechatBindRequest();
        r.setBindToken("bt");
        r.setMobile("13800000000");
        r.setCode("123456");
        return r;
    }

    @Test
    void 绑定_校验验证码_写入绑定关系_登录() {
        when(ops.get(MemberWechatAuthServiceImpl.BIND_PREFIX + "bt")).thenReturn("openid-1|union-1");
        when(authService.verifySmsAndGetOrRegister("13800000000", "123456")).thenReturn(1001L);

        MemberTokenVO t = service.bind(bindReq());

        ArgumentCaptor<MbrSocial> s = ArgumentCaptor.forClass(MbrSocial.class);
        verify(socialMapper).insert(s.capture());
        assertThat(s.getValue().getMemberId()).isEqualTo(1001L);
        assertThat(s.getValue().getOpenid()).isEqualTo("openid-1");
        assertThat(s.getValue().getUnionid()).isEqualTo("union-1");
        assertThat(s.getValue().getPlatform()).isEqualTo("WECHAT_MP");
        verify(redis).delete(startsWith(MemberWechatAuthServiceImpl.BIND_PREFIX));
        assertThat(t).isSameAs(token);
    }

    @Test
    void 绑定_bindToken过期() {
        when(ops.get(anyString())).thenReturn(null);
        assertThatThrownBy(() -> service.bind(bindReq())).hasMessageContaining("绑定已过期");
        verify(authService, never()).verifySmsAndGetOrRegister(anyString(), anyString());
    }

    @Test
    void 绑定_该微信已绑定其他账号() {
        when(ops.get(anyString())).thenReturn("openid-1|");
        when(authService.verifySmsAndGetOrRegister(anyString(), anyString())).thenReturn(1001L);
        MbrSocial other = new MbrSocial();
        other.setMemberId(2002L);
        when(socialMapper.selectOne(any())).thenReturn(other);

        assertThatThrownBy(() -> service.bind(bindReq())).hasMessageContaining("已绑定其他账号");
        verify(socialMapper, never()).insert(any(MbrSocial.class));
    }

    @Test
    void 绑定_手机号已绑定其他微信() {
        when(ops.get(anyString())).thenReturn("openid-1|");
        when(authService.verifySmsAndGetOrRegister(anyString(), anyString())).thenReturn(1001L);
        MbrSocial mine = new MbrSocial();
        mine.setMemberId(1001L);
        mine.setOpenid("openid-old");
        when(socialMapper.selectOne(any())).thenReturn(null, mine);

        assertThatThrownBy(() -> service.bind(bindReq())).hasMessageContaining("已绑定其他微信");
    }

    @Test
    void 绑定_并发重复绑定由唯一索引兜底() {
        when(ops.get(anyString())).thenReturn("openid-1|");
        when(authService.verifySmsAndGetOrRegister(anyString(), anyString())).thenReturn(1001L);
        when(socialMapper.insert(any(MbrSocial.class))).thenThrow(new DuplicateKeyException("dup"));

        assertThatThrownBy(() -> service.bind(bindReq())).isInstanceOf(BusinessException.class)
                .hasMessageContaining("已绑定其他账号");
    }
}

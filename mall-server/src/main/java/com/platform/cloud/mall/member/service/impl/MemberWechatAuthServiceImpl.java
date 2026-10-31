package com.platform.cloud.mall.member.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.member.api.MemberApi;
import com.platform.cloud.mall.member.dto.WechatBindRequest;
import com.platform.cloud.mall.member.entity.MbrSocial;
import com.platform.cloud.mall.member.mapper.MbrSocialMapper;
import com.platform.cloud.mall.member.service.MemberAuthService;
import com.platform.cloud.mall.member.service.MemberWechatAuthService;
import com.platform.cloud.mall.member.support.WechatMpClient;
import com.platform.cloud.mall.member.support.WechatMpClient.OAuthResult;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.member.vo.WechatAuthorizeVO;
import com.platform.cloud.mall.member.vo.WechatLoginVO;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * 微信公众号网页授权登录。
 * <p>
 * 安全要点：
 * <ul>
 *   <li>state 随机生成存 Redis、一次性消费，防止攻击者诱导用户用攻击者的 code 登录（登录 CSRF）；</li>
 *   <li>openid 不下发给前端，未绑定时只给一个 10 分钟有效的随机 bindToken，绑定时凭它从 Redis 取回 openid，
 *       防止伪造 openid 冒绑他人微信；</li>
 *   <li>令牌签发复用 {@link MemberAuthService#loginTrusted}，与密码 / 验证码登录的令牌声明一致。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MemberWechatAuthServiceImpl implements MemberWechatAuthService {

    static final String STATE_PREFIX = "mall:wx:oauth:state:";
    static final String BIND_PREFIX = "mall:wx:oauth:bind:";
    /** 授权页停留 + 回跳的时间，10 分钟足够；code 本身 5 分钟过期 */
    static final Duration STATE_TTL = Duration.ofMinutes(10);
    /** 用户收验证码、填写的时间 */
    static final Duration BIND_TTL = Duration.ofMinutes(10);
    static final String AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize";
    /** Redis 里 openid 与 unionid 的分隔符，二者都不含该字符 */
    private static final char SEP = '|';
    private static final int REDIRECT_MAX = 512;

    private final ConfigApi configApi;
    private final WechatMpClient mpClient;
    private final MbrSocialMapper socialMapper;
    private final MemberAuthService authService;
    private final StringRedisTemplate redis;

    @Override
    public WechatAuthorizeVO authorizeUrl(String redirect) {
        String appId = requireAppId();
        if (!StringUtils.hasText(redirect) || redirect.length() > REDIRECT_MAX
                || !(redirect.startsWith("https://") || redirect.startsWith("http://"))) {
            throw new BusinessException("回跳地址不正确");
        }
        String state = randomToken();
        redis.opsForValue().set(STATE_PREFIX + state, "1", STATE_TTL);
        // snsapi_base 静默授权：用户无感知，只能拿 openid；昵称头像由用户在商城内自行设置
        String url = AUTHORIZE_URL + "?appid=" + enc(appId) + "&redirect_uri=" + enc(redirect)
                + "&response_type=code&scope=snsapi_base&state=" + state + "#wechat_redirect";
        return new WechatAuthorizeVO(url, state);
    }

    @Override
    public WechatLoginVO login(String code, String state) {
        // delete 返回 true 才说明 state 由本系统下发且未被用过：一次性，重放无效
        if (!Boolean.TRUE.equals(redis.delete(STATE_PREFIX + state))) {
            throw new BusinessException("授权已过期，请重新进入");
        }
        String appId = requireAppId();
        String secret = configApi.getString(ConfigKeys.WECHAT_MP_APP_SECRET, "");
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException("商城未开启微信登录");
        }
        OAuthResult r = mpClient.exchangeCode(appId, secret.trim(), code);
        MbrSocial bound = findByOpenid(r.openid());
        if (bound != null) {
            return WechatLoginVO.loggedIn(authService.loginTrusted(bound.getMemberId(), "微信授权登录"));
        }
        String bindToken = randomToken();
        redis.opsForValue().set(BIND_PREFIX + bindToken, r.openid() + SEP + r.unionid(), BIND_TTL);
        return WechatLoginVO.needBind(bindToken);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MemberTokenVO bind(WechatBindRequest req) {
        String key = BIND_PREFIX + req.getBindToken();
        String value = redis.opsForValue().get(key);
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("绑定已过期，请重新进入商城");
        }
        int sep = value.indexOf(SEP);
        String openid = sep < 0 ? value : value.substring(0, sep);
        String unionid = sep < 0 ? "" : value.substring(sep + 1);

        Long memberId = authService.verifySmsAndGetOrRegister(req.getMobile(), req.getCode());
        MbrSocial byOpenid = findByOpenid(openid);
        if (byOpenid != null && !Objects.equals(byOpenid.getMemberId(), memberId)) {
            throw new BusinessException("该微信已绑定其他账号");
        }
        if (byOpenid == null) {
            MbrSocial byMember = socialMapper.selectOne(new LambdaQueryWrapper<MbrSocial>()
                    .eq(MbrSocial::getMemberId, memberId)
                    .eq(MbrSocial::getPlatform, MemberApi.PLATFORM_WECHAT_MP));
            if (byMember != null) {
                // 换绑需先解绑旧微信，不能静默覆盖：否则拿到手机验证码就能把账号原来的微信顶掉
                throw new BusinessException("该手机号已绑定其他微信");
            }
            MbrSocial s = new MbrSocial();
            s.setMemberId(memberId);
            s.setPlatform(MemberApi.PLATFORM_WECHAT_MP);
            s.setOpenid(openid);
            s.setUnionid(unionid);
            try {
                socialMapper.insert(s);
            } catch (DuplicateKeyException e) {
                // 同一微信并发绑定，唯一索引兜底
                throw new BusinessException("该微信已绑定其他账号");
            }
        }
        redis.delete(key);
        return authService.loginTrusted(memberId, "微信绑定手机号登录");
    }

    private MbrSocial findByOpenid(String openid) {
        return socialMapper.selectOne(new LambdaQueryWrapper<MbrSocial>()
                .eq(MbrSocial::getPlatform, MemberApi.PLATFORM_WECHAT_MP)
                .eq(MbrSocial::getOpenid, openid));
    }

    private String requireAppId() {
        String appId = configApi.getString(ConfigKeys.WECHAT_MP_APP_ID, "");
        if (!StringUtils.hasText(appId)) {
            throw new BusinessException("商城未开启微信登录");
        }
        return appId.trim();
    }

    private static String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}

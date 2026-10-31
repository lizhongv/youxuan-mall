package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.dto.MemberLoginRequest;
import com.platform.cloud.mall.member.dto.MemberRegisterRequest;
import com.platform.cloud.mall.member.dto.MemberResetPasswordRequest;
import com.platform.cloud.mall.member.dto.SmsLoginRequest;
import com.platform.cloud.mall.member.vo.MemberTokenVO;

/**
 * 商城端认证。
 */
public interface MemberAuthService {

    MemberTokenVO register(MemberRegisterRequest req);

    /** 密码登录；连续失败 2 次后需要图形验证码，5 次锁定该 IP 15 分钟（验证码登录不受影响） */
    MemberTokenVO login(MemberLoginRequest req);

    /** 验证码登录，未注册的手机号自动注册 */
    MemberTokenVO loginBySms(SmsLoginRequest req);

    MemberTokenVO refresh(String refreshToken);

    /** 验证码重置密码：同时作废该会员所有已签发令牌 */
    void resetPassword(MemberResetPasswordRequest req);

    /** 登出：access 与 refresh 令牌都拉黑到过期；refreshToken 可空 */
    void logout(String accessToken, String refreshToken);

    /**
     * 为已通过其他方式认证的会员签发令牌对（写入当前令牌版本号）。
     * 供会员领域内部及第三方登录（如微信网页授权）调用；调用方负责确认会员存在且未冻结。
     */
    MemberTokenVO issueTokens(Long memberId);

    /**
     * 已由可信第三方确认身份的会员直接登录（如微信 openid 已绑定）：校验冻结、记登录日志、签发令牌。
     * 令牌与密码 / 验证码登录走同一个签发方法，令牌声明保持一致。
     */
    MemberTokenVO loginTrusted(Long memberId, String message);

    /**
     * 校验登录验证码并按手机号取会员，未注册则自动注册（第三方账号绑定手机号使用）。
     *
     * @return 会员 ID；账号冻结时抛业务异常
     */
    Long verifySmsAndGetOrRegister(String mobile, String code);
}

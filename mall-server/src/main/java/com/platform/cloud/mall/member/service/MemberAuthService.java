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

    MemberTokenVO login(MemberLoginRequest req);

    /** 验证码登录，未注册的手机号自动注册 */
    MemberTokenVO loginBySms(SmsLoginRequest req);

    MemberTokenVO refresh(String refreshToken);

    void resetPassword(MemberResetPasswordRequest req);

    /** 登出：access 令牌拉黑到过期 */
    void logout(String accessToken);

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

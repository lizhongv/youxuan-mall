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
}

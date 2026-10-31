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
}

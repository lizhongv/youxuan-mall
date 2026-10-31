package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.dto.WechatBindRequest;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.member.vo.WechatAuthorizeVO;
import com.platform.cloud.mall.member.vo.WechatLoginVO;

/**
 * 微信公众号网页授权登录（snsapi_base 静默授权，只取 openid）。
 * <p>
 * 流程：authorize-url → 用户在微信内跳转授权 → 回跳带 code/state → login；
 * openid 未绑定会员时返回 bindToken，再用手机号验证码 bind（未注册自动注册）。
 */
public interface MemberWechatAuthService {

    /** @param redirect 授权后回跳的商城页面完整地址，域名须在公众号「网页授权域名」中 */
    WechatAuthorizeVO authorizeUrl(String redirect);

    WechatLoginVO login(String code, String state);

    MemberTokenVO bind(WechatBindRequest req);
}

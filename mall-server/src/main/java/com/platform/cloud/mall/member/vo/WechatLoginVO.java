package com.platform.cloud.mall.member.vo;

import lombok.Data;

/**
 * 微信登录结果：已绑定会员直接返回令牌；未绑定返回 needBind = true 与一次性 bindToken，
 * 前端引导「手机号 + 验证码」绑定。
 */
@Data
public class WechatLoginVO {

    private Boolean needBind;
    private String bindToken;
    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;

    public static WechatLoginVO loggedIn(MemberTokenVO t) {
        WechatLoginVO vo = new WechatLoginVO();
        vo.setNeedBind(false);
        vo.setAccessToken(t.getAccessToken());
        vo.setRefreshToken(t.getRefreshToken());
        vo.setExpiresIn(t.getExpiresIn());
        return vo;
    }

    public static WechatLoginVO needBind(String bindToken) {
        WechatLoginVO vo = new WechatLoginVO();
        vo.setNeedBind(true);
        vo.setBindToken(bindToken);
        return vo;
    }
}

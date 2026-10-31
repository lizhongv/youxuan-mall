package com.platform.cloud.mall.member.support;

/**
 * 微信公众号开放接口。单独抽成接口是为了单测时替换掉 HTTP 调用。
 */
public interface WechatMpClient {

    /**
     * 网页授权 code 换 openid（sns/oauth2/access_token）。code 只能用一次、5 分钟内有效。
     *
     * @throws com.platform.cloud.bootstrap.common.exception.BusinessException 微信返回错误码或网络失败
     */
    OAuthResult exchangeCode(String appId, String appSecret, String code);

    /** @param unionid 公众号绑定到开放平台后才有，否则为空串 */
    record OAuthResult(String openid, String unionid) {
    }
}

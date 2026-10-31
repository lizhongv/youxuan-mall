package com.platform.cloud.mall.member.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 公众号网页授权返回解析 */
class HttpWechatMpClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void 微信返回错误码时提示重新授权() {
        assertThatThrownBy(() -> HttpWechatMpClient.parse(mapper, "{\"errcode\":40163,\"errmsg\":\"code been used\"}"))
                .hasMessageContaining("重新进入");
    }

    @Test
    void 成功返回openid与unionid() {
        WechatMpClient.OAuthResult r = HttpWechatMpClient.parse(mapper, "{\"openid\":\"o1\",\"unionid\":\"u1\"}");
        assertThat(r.openid()).isEqualTo("o1");
        assertThat(r.unionid()).isEqualTo("u1");
    }
}

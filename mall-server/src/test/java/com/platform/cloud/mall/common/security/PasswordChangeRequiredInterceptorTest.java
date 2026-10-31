package com.platform.cloud.mall.common.security;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.security.JwtAuthenticationFilter;
import com.platform.cloud.mall.common.config.MallProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 强制改密：带 mcp 声明的管理员令牌访问普通接口返回 40301。
 */
class PasswordChangeRequiredInterceptorTest {

    private static MockHttpServletRequest request(String utype, Boolean mcp) {
        var b = Jwts.claims().subject("1").add("utype", utype);
        if (mcp != null) {
            b.add("mcp", mcp);
        }
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/admin/goods/page");
        req.setAttribute(JwtAuthenticationFilter.CLAIMS_ATTRIBUTE, b.build());
        return req;
    }

    @Test
    void mustChangeAdminIsBlocked() {
        PasswordChangeRequiredInterceptor i = new PasswordChangeRequiredInterceptor(new MallProperties());

        assertThatThrownBy(() -> i.preHandle(request("ADMIN", true), new MockHttpServletResponse(), new Object()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode()).isEqualTo(40301))
                .hasMessage("请先修改初始密码");
    }

    @Test
    void normalAdminPasses() throws Exception {
        PasswordChangeRequiredInterceptor i = new PasswordChangeRequiredInterceptor(new MallProperties());
        assertThat(i.preHandle(request("ADMIN", null), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(i.preHandle(request("MEMBER", true), new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(i.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void canBeDisabledForDemo() throws Exception {
        MallProperties props = new MallProperties();
        props.getSecurity().setForceChangeInitialPassword(false);
        PasswordChangeRequiredInterceptor i = new PasswordChangeRequiredInterceptor(props);
        assertThat(i.preHandle(request("ADMIN", true), new MockHttpServletResponse(), new Object())).isTrue();
    }

    @Test
    void passwordChangeEndpointsAreWhitelisted() {
        assertThat(PasswordChangeRequiredInterceptor.ALLOWED_PATHS)
                .contains("/admin/auth/me", "/admin/auth/password", "/admin/auth/logout");
    }
}

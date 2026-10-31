package com.platform.cloud.mall.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.DeferredLogs;
import org.springframework.mock.env.MockEnvironment;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 启动安全检查与 prod 强制开关：默认配置不能直接上线。
 */
class StartupSafetyCheckTest {

    private static final String GOOD_SECRET = "Zq3v9Yp2Lr8Kx1Wm5Tn7Bc4Hd6Fg0Js2Qa9Ue3Io8Pl";

    private static Map<String, String> safeProps() {
        Map<String, String> m = new HashMap<>();
        m.put("platform.jwt.secret", GOOD_SECRET);
        m.put("spring.datasource.password", "Xk2!pq9Lm3vR");
        m.put("spring.data.redis.password", "Rz8@lk2Mn5qW");
        return m;
    }

    @Test
    void profileDefaultsToProd() {
        assertThat(StartupSafetyCheck.isDemo(new String[0])).isFalse();
        assertThat(StartupSafetyCheck.isDemo(new String[]{"dev"})).isFalse();
        assertThat(StartupSafetyCheck.isDemo(new String[]{"demo"})).isTrue();
        // 同时写了 demo 与 prod 时取更安全的一边
        assertThat(StartupSafetyCheck.isDemo(new String[]{"demo", "prod"})).isFalse();
    }

    @Test
    void safeConfigPasses() {
        assertThat(StartupSafetyCheck.check(false, safeProps()::get).ok()).isTrue();
    }

    @Test
    void prodRejectsKnownDefaultJwtSecret() {
        Map<String, String> p = safeProps();
        p.put("platform.jwt.secret", "mall-dev-only-jwt-secret-please-change-in-production-32b");
        StartupSafetyCheck.Result r = StartupSafetyCheck.check(false, p::get);
        assertThat(r.ok()).isFalse();
        assertThat(r.describe()).contains("MALL_JWT_SECRET").contains("公开默认值");
    }

    @Test
    void prodRejectsEnvExamplePlaceholderAndShortSecret() {
        Map<String, String> p = safeProps();
        p.put("platform.jwt.secret", "change-me-to-a-random-string-at-least-32-bytes");
        assertThat(StartupSafetyCheck.check(false, p::get).ok()).isFalse();
        p.put("platform.jwt.secret", "short-but-random-Xq9");
        assertThat(StartupSafetyCheck.check(false, p::get).errors()).anyMatch(e -> e.contains("32 字节"));
        p.remove("platform.jwt.secret");
        assertThat(StartupSafetyCheck.check(false, p::get).errors()).anyMatch(e -> e.contains("未配置"));
    }

    @Test
    void prodRejectsPlaceholderDatabaseAndRedisPasswords() {
        Map<String, String> p = safeProps();
        p.put("spring.datasource.password", "change-me-mall");
        p.put("spring.data.redis.password", "change-me-redis");
        StartupSafetyCheck.Result r = StartupSafetyCheck.check(false, p::get);
        assertThat(r.errors()).hasSize(2);

        p = safeProps();
        p.put("spring.datasource.password", "mall123");
        assertThat(StartupSafetyCheck.check(false, p::get).ok()).isFalse();
    }

    @Test
    void emptyRedisPasswordOnlyWarns() {
        Map<String, String> p = safeProps();
        p.remove("spring.data.redis.password");
        StartupSafetyCheck.Result r = StartupSafetyCheck.check(false, p::get);
        assertThat(r.ok()).isTrue();
        assertThat(r.warnings()).anyMatch(w -> w.contains("Redis"));
    }

    @Test
    void demoOnlyWarns() {
        Map<String, String> p = new HashMap<>();
        p.put("platform.jwt.secret", "mall-demo-only-jwt-secret-never-use-in-production-0123456789");
        p.put("spring.datasource.password", "mall123");
        StartupSafetyCheck.Result r = StartupSafetyCheck.check(true, p::get);
        assertThat(r.ok()).isTrue();
        assertThat(r.warnings()).anyMatch(w -> w.contains("demo"));
    }

    @Test
    void postProcessorForcesDemoSwitchesOffInProd() {
        MockEnvironment env = new MockEnvironment();
        safeProps().forEach(env::setProperty);
        // .env 里遗留的演示开关不能在 prod 生效
        env.setProperty("mall.pay.mock-enabled", "true");
        env.setProperty("mall.sms.dev-mode", "true");
        env.setProperty("knife4j.production", "false");

        new MallProfileEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(env, null);

        assertThat(env.getProperty("mall.pay.mock-enabled")).isEqualTo("false");
        assertThat(env.getProperty("mall.sms.dev-mode")).isEqualTo("false");
        assertThat(env.getProperty("mall.security.captcha-echo")).isEqualTo("false");
        assertThat(env.getProperty("mall.security.force-change-initial-password")).isEqualTo("true");
        assertThat(env.getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(env.getProperty("knife4j.production")).isEqualTo("true");
        // 关闭 api-docs 时 knife4j 必须一并关闭，否则其自动配置缺少 SpringDocConfigProperties 导致启动失败
        assertThat(env.getProperty("knife4j.enable")).isEqualTo("false");
    }

    @Test
    void postProcessorKeepsDemoSwitchesInDemo() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("demo");
        env.setProperty("mall.pay.mock-enabled", "true");

        new MallProfileEnvironmentPostProcessor(new DeferredLogs()).postProcessEnvironment(env, null);

        assertThat(env.getProperty("mall.pay.mock-enabled")).isEqualTo("true");
    }

    @Test
    void postProcessorRefusesToStartProdWithDefaults() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("platform.jwt.secret", "mall-dev-only-jwt-secret-please-change-in-production-32b");
        env.setProperty("spring.datasource.password", "change-me-mall");

        assertThatThrownBy(() -> new MallProfileEnvironmentPostProcessor(new DeferredLogs())
                .postProcessEnvironment(env, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("拒绝在 prod 模式下启动");
    }
}

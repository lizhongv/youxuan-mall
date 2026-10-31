package com.platform.cloud.mall.common.config;

import org.apache.commons.logging.Log;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * 运行模式（demo / prod）落地：在 Spring 容器创建之前执行。
 * <ol>
 *   <li>非 demo（含未配置）一律视为 prod，以最高优先级覆盖演示开关：模拟支付、验证码回显、接口文档全部关闭。
 *       放在最高优先级是为了让 .env 里遗留的 MALL_PAY_MOCK_ENABLED=true 之类配置也无法在 prod 下生效。</li>
 *   <li>执行 {@link StartupSafetyCheck}：prod 下密钥 / 口令为公开默认值时直接拒绝启动。
 *       放在这里而不是某个 Bean 里，是为了先于 JwtService 等 Bean 初始化报错，提示更直接。</li>
 * </ol>
 * 通过 META-INF/spring.factories 注册；排在 ConfigData 处理之后，能读到 application-*.yml 与环境变量。
 */
public class MallProfileEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String OVERRIDES_SOURCE = "mallProdSafetyOverrides";

    /**
     * prod 模式强制值。
     * 关闭 api-docs 后 springdoc 不再注册 SpringDocConfigProperties，而 knife4j 的自动配置依赖它，
     * 因此 knife4j.enable 一并关闭（否则启动失败）；doc.html 静态页由 MallSecurityConfig 不再放行。
     */
    static final Map<String, Object> PROD_OVERRIDES = Map.of(
            "mall.pay.mock-enabled", "false",
            "mall.sms.dev-mode", "false",
            "mall.security.captcha-echo", "false",
            "mall.security.force-change-initial-password", "true",
            "springdoc.api-docs.enabled", "false",
            "springdoc.swagger-ui.enabled", "false",
            "knife4j.enable", "false",
            "knife4j.production", "true");

    private final Log log;

    public MallProfileEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(MallProfileEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication application) {
        boolean demo = StartupSafetyCheck.isDemo(env.getActiveProfiles());
        if (!demo) {
            env.getPropertySources().addFirst(new MapPropertySource(OVERRIDES_SOURCE, PROD_OVERRIDES));
        }
        StartupSafetyCheck.Result result = StartupSafetyCheck.check(demo, env::getProperty);
        result.warnings().forEach(w -> log.warn("[启动检查] " + w));
        if (!result.ok()) {
            String msg = result.describe();
            // 日志系统此时尚未初始化，直接写 stderr，保证 docker compose logs 里能看到
            System.err.println(msg);
            throw new IllegalStateException(msg);
        }
        log.info("[启动检查] 运行模式: " + (demo ? StartupSafetyCheck.PROFILE_DEMO : StartupSafetyCheck.PROFILE_PROD));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

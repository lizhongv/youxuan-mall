package com.platform.cloud.mall.common.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * 启动安全检查（纯函数，便于单测）。
 * <p>
 * prod 模式下发现「公开已知的密钥 / 口令」直接拒绝启动：这类配置一旦带到公网，任何人都能伪造管理员令牌或登录数据库，
 * 而且问题不会在功能测试里暴露，只能在启动时卡住。demo 模式只给出警告。
 */
public final class StartupSafetyCheck {

    public static final String PROFILE_DEMO = "demo";
    public static final String PROFILE_PROD = "prod";

    /** HS256 密钥最小长度：低于 256 位可被离线暴力破解 */
    static final int MIN_JWT_SECRET_BYTES = 32;

    /** 仓库里出现过的默认 JWT 密钥（application.yml、.env.example、demo 配置），都视为已公开 */
    static final Set<String> KNOWN_JWT_SECRETS = Set.of(
            "mall-dev-only-jwt-secret-please-change-in-production-32b",
            "mall-demo-only-jwt-secret-never-use-in-production-0123456789",
            "change-me-to-a-random-string-at-least-32-bytes");

    /** 常见弱口令 / 仓库默认口令 */
    static final Set<String> WEAK_PASSWORDS = Set.of("mall123", "123456", "root", "password", "admin", "admin123");

    private static final String PLACEHOLDER_PREFIX = "change-me";

    private StartupSafetyCheck() {
    }

    /** 检查结果：errors 非空时拒绝启动 */
    public record Result(List<String> errors, List<String> warnings) {

        public boolean ok() {
            return errors.isEmpty();
        }

        /** 拼成多行可读提示，直接打印在控制台 */
        public String describe() {
            StringBuilder sb = new StringBuilder("商城服务端拒绝在 prod 模式下启动，请修正以下配置：");
            for (int i = 0; i < errors.size(); i++) {
                sb.append(System.lineSeparator()).append("  ").append(i + 1).append(". ").append(errors.get(i));
            }
            sb.append(System.lineSeparator())
                    .append("  （仅演示环境可设置 MALL_PROFILE=demo 跳过；详见 deploy/mall/README.md「运行模式」）");
            return sb.toString();
        }
    }

    /** 未显式指定 demo 一律按 prod 处理：忘记配置时宁可「太安全」而不是把演示开关带到线上 */
    public static boolean isDemo(String[] activeProfiles) {
        List<String> profiles = activeProfiles == null ? List.of() : Arrays.asList(activeProfiles);
        return profiles.contains(PROFILE_DEMO) && !profiles.contains(PROFILE_PROD);
    }

    /**
     * @param demo     是否 demo 模式
     * @param property 配置读取函数（Environment::getProperty），返回 null 表示未配置
     */
    public static Result check(boolean demo, Function<String, String> property) {
        List<String> problems = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String jwt = property.apply("platform.jwt.secret");
        if (jwt == null || jwt.isBlank()) {
            problems.add("未配置 JWT 签名密钥：请设置环境变量 MALL_JWT_SECRET（≥ 32 字节随机串，可用 openssl rand -base64 48 生成）");
        } else if (KNOWN_JWT_SECRETS.contains(jwt) || isPlaceholder(jwt)) {
            problems.add("MALL_JWT_SECRET 仍是仓库中的公开默认值，任何人都能伪造登录令牌，请替换为随机串");
        } else if (jwt.getBytes(StandardCharsets.UTF_8).length < MIN_JWT_SECRET_BYTES) {
            problems.add("MALL_JWT_SECRET 长度不足 " + MIN_JWT_SECRET_BYTES + " 字节，请替换为更长的随机串");
        }

        String dbPassword = property.apply("spring.datasource.password");
        if (dbPassword == null || dbPassword.isBlank()) {
            problems.add("数据库密码为空：请设置 MYSQL_PASSWORD");
        } else if (isPlaceholder(dbPassword) || isWeak(dbPassword)) {
            problems.add("数据库密码（MYSQL_PASSWORD）是占位值或弱口令，请替换为随机串");
        }

        String redisPassword = property.apply("spring.data.redis.password");
        if (redisPassword != null && (isPlaceholder(redisPassword) || isWeak(redisPassword))) {
            problems.add("Redis 密码（REDIS_PASSWORD）是占位值或弱口令，请替换为随机串");
        } else if (redisPassword == null || redisPassword.isBlank()) {
            // 本机回环上的无密码 Redis 是合法部署方式，不强制，只提醒
            warnings.add("Redis 未设置密码（REDIS_PASSWORD），请确认 Redis 只监听本机或内网");
        }

        String initAdmin = property.apply("mall.init-admin-password");
        if (initAdmin != null && (isWeak(initAdmin) || isPlaceholder(initAdmin))) {
            // 只在首次建库时生效，且首次登录会被强制修改，因此不拒绝启动
            warnings.add("MALL_INIT_ADMIN_PASSWORD 为弱口令：仅首次建库时使用，admin 首次登录后会被要求修改");
        }

        if (demo) {
            // demo 模式不拒绝启动，但把问题全部列为警告，避免演示机被误当成生产环境
            warnings.addAll(0, problems.stream().map(p -> "[demo 模式未拦截] " + p).toList());
            warnings.add("当前为 demo 模式：可能开启模拟支付、验证码回显与接口文档，切勿用于真实营业");
            return new Result(List.of(), List.copyOf(warnings));
        }
        return new Result(List.copyOf(problems), List.copyOf(warnings));
    }

    private static boolean isPlaceholder(String v) {
        return v.trim().toLowerCase(Locale.ROOT).startsWith(PLACEHOLDER_PREFIX);
    }

    private static boolean isWeak(String v) {
        return WEAK_PASSWORDS.contains(v.trim().toLowerCase(Locale.ROOT));
    }
}

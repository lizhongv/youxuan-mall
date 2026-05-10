package com.platform.cloud.bootstrap.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "platform.jwt")
public class JwtProperties {

    /** HS256 密钥；生产环境必须 ≥ 32 字节并通过环境变量注入 */
    private String secret;

    /** access token 过期时间，默认 30 分钟 */
    private Duration accessTtl = Duration.ofMinutes(30);

    /** refresh token 过期时间，默认 7 天 */
    private Duration refreshTtl = Duration.ofDays(7);

    /** 签发者 */
    private String issuer = "platform-cloud";

    /** Authorization 头名 */
    private String header = "Authorization";

    /** Token 前缀 */
    private String tokenPrefix = "Bearer ";
}

package com.platform.cloud.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * auth-service 独立启动入口。
 * <p>放在 com.platform.cloud.auth 包下，与 bootstrap-app 的 BootstrapApplication 隔离，
 * 通过 scanBasePackages 显式拉入 com.platform.cloud.bootstrap.* 下的所有 starter 组件。
 */
@SpringBootApplication(scanBasePackages = {"com.platform.cloud.bootstrap", "com.platform.cloud.auth"})
@ConfigurationPropertiesScan(basePackages = "com.platform.cloud.bootstrap")
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}

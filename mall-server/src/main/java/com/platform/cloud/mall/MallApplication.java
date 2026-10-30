package com.platform.cloud.mall;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 商城服务端入口。
 * <p>
 * 同时扫描 starter 的 {@code com.platform.cloud.bootstrap.common} 包，复用统一响应、全局异常、JWT、幂等、多级缓存等基础设施。
 */
@EnableScheduling
@ConfigurationPropertiesScan({"com.platform.cloud.mall", "com.platform.cloud.bootstrap.common"})
@SpringBootApplication(scanBasePackages = {"com.platform.cloud.mall", "com.platform.cloud.bootstrap.common"})
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
    }
}

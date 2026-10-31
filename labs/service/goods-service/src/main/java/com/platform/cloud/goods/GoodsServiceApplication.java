package com.platform.cloud.goods;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.platform.cloud.bootstrap", "com.platform.cloud.goods"})
@ConfigurationPropertiesScan(basePackages = "com.platform.cloud.bootstrap")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.platform.cloud.goods.rpc")
public class GoodsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoodsServiceApplication.class, args);
    }
}

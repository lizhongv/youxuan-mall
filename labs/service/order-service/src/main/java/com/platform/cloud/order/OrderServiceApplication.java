package com.platform.cloud.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.platform.cloud.bootstrap", "com.platform.cloud.order"})
@ConfigurationPropertiesScan(basePackages = "com.platform.cloud.bootstrap")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.platform.cloud.order.rpc")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}

package com.platform.cloud.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = "com.platform.cloud.bootstrap")
public class BootstrapApplication {

    public static void main(String[] args) {
        SpringApplication.run(BootstrapApplication.class, args);
    }
}

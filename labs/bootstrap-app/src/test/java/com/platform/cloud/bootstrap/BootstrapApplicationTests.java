package com.platform.cloud.bootstrap;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BootstrapApplicationTests {

    @Test
    void contextLoads() {
        // Phase 1 烟雾测试：能加载 Spring 上下文即通过
        // Phase 1 中后期补 Testcontainers + 真实 MySQL/Redis 集成测试
    }
}

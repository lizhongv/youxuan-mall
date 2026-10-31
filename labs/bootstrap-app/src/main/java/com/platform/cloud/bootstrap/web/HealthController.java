package com.platform.cloud.bootstrap.web;

import com.platform.cloud.bootstrap.common.web.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@Tag(name = "Health", description = "健康检查与基础探针")
@RestController
@RequestMapping("/api")
public class HealthController {

    @Operation(summary = "应用 ping 探针")
    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        return R.ok(Map.of(
                "app", "bootstrap-app",
                "status", "UP",
                "time", OffsetDateTime.now().toString(),
                "javaVersion", System.getProperty("java.version")
        ));
    }
}

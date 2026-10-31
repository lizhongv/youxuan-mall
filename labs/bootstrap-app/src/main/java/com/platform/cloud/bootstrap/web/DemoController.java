package com.platform.cloud.bootstrap.web;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.idempotent.Idempotent;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.bootstrap.common.web.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 1.1 基础设施层验证用 demo。Phase 1.3 起会被业务接口替换。
 */
@Tag(name = "Demo", description = "基础设施层验证（异常/校验/幂等）")
@Validated
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    @Operation(summary = "抛出业务异常")
    @GetMapping("/business-error")
    public R<Void> businessError() {
        throw new BusinessException(ResultCode.BUSINESS_ERROR, "演示用业务异常");
    }

    @Operation(summary = "未捕获异常")
    @GetMapping("/runtime-error")
    public R<Void> runtimeError() {
        throw new IllegalStateException("演示用未捕获异常");
    }

    @Operation(summary = "URL 参数校验：age 必须 >= 1")
    @GetMapping("/validate-param")
    public R<Integer> validateParam(@RequestParam @Min(value = 1, message = "必须大于等于 1") int age) {
        return R.ok(age);
    }

    @Operation(summary = "请求体校验：name 非空且 1-20 字符")
    @PostMapping("/validate-body")
    public R<DemoForm> validateBody(@Valid @RequestBody DemoForm form) {
        return R.ok(form);
    }

    @Operation(summary = "幂等接口：同一 X-Idempotent-Key 60 秒内只能成功一次")
    @Idempotent(prefix = "demo:idem:", ttl = 60)
    @PostMapping("/idempotent")
    public R<String> idempotent() {
        return R.ok("ok-" + System.currentTimeMillis());
    }

    @Data
    public static class DemoForm {
        @NotBlank(message = "不能为空")
        @Size(min = 1, max = 20, message = "长度需 1-20")
        private String name;

        @Min(value = 0, message = "不能小于 0")
        private int amount;
    }
}

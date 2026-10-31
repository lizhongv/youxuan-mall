package com.platform.cloud.mall.member.controller.admin;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.member.dto.SmsTestRequest;
import com.platform.cloud.mall.member.service.NotifySmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 短信渠道测试。路径挂在 /admin/system 下，与系统参数页同一处配置、同一个权限；
 * 实现放在 member 领域，因为短信发送能力（SmsSender）归属会员领域。
 */
@Tag(name = "系统参数-短信")
@RestController
@RequestMapping("/admin/system/sms")
@RequiredArgsConstructor
public class AdminSmsController {

    private final NotifySmsService notifySmsService;

    @Operation(summary = "测试发送（用验证码模板发送一条随机验证码）")
    @PreAuthorize("@ss.has('system:config:edit')")
    @PostMapping("/test")
    public R<Map<String, String>> test(@Valid @RequestBody SmsTestRequest req) {
        return R.ok(Map.of("provider", notifySmsService.sendTest(req.getMobile()).name()));
    }
}

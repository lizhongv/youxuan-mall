package com.platform.cloud.mall.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 后台测试发送短信 */
@Data
public class SmsTestRequest {

    @NotBlank(message = "请填写手机号")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    @Schema(description = "接收测试短信的手机号")
    private String mobile;
}

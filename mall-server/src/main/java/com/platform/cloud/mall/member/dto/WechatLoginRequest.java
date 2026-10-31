package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 微信网页授权回跳后用 code 登录 */
@Data
public class WechatLoginRequest {

    @NotBlank
    @Size(max = 128)
    private String code;

    /** 获取授权地址时下发的 state，防 CSRF */
    @NotBlank
    @Size(max = 64)
    private String state;
}

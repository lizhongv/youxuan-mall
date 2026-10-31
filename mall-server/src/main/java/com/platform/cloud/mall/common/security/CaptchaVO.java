package com.platform.cloud.mall.common.security;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图形验证码。image 为 data URI（data:image/png;base64,...），前端直接放进 img 的 src。
 * devCode 只在 demo 模式且开启 mall.security.captcha-echo 时返回，供冒烟测试脚本使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CaptchaVO {

    private String captchaId;
    private String image;
    private String devCode;
}

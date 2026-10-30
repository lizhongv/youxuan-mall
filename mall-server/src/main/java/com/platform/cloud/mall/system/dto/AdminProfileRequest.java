package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员修改个人资料 */
@Data
public class AdminProfileRequest {

    @Size(max = 64)
    private String nickname;

    @Size(max = 255)
    private String avatar;

    /** 允许留空表示不填写 */
    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @Email
    @Size(max = 100)
    private String email;
}

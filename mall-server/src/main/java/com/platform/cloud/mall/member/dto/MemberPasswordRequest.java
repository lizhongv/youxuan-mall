package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 会员修改密码。验证码注册的会员还没有密码，此时 oldPassword 可不传，相当于首次设置密码。
 */
@Data
public class MemberPasswordRequest {

    private String oldPassword;

    @NotBlank
    @Size(min = 6, max = 32, message = "密码长度需为 6~32 位")
    private String newPassword;
}

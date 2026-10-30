package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 新增 / 修改管理员。修改时忽略 username 与 password（密码走单独的重置接口） */
@Data
public class AdminSaveRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_]{3,32}$", message = "用户名为 3~32 位字母、数字或下划线")
    private String username;

    /** 修改时前端可能传空串，因此允许空；新增时在业务层要求必填 */
    @Pattern(regexp = "^$|^.{6,32}$", message = "密码长度需为 6~32 位")
    private String password;

    @Size(max = 64)
    private String nickname;

    @Size(max = 255)
    private String avatar;

    @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
    private String mobile;

    @Email
    @Size(max = 100)
    private String email;

    @Min(0)
    @Max(1)
    private Integer status;

    private List<Long> roleIds;

    @Size(max = 255)
    private String remark;
}

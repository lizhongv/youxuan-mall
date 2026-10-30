package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增 / 修改角色 */
@Data
public class RoleSaveRequest {

    @NotBlank
    @Size(max = 64)
    private String name;

    /** 编码会拼成令牌里的 ROLE_xxx，限定字符集避免与权限码（含冒号）混淆 */
    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,47}$", message = "编码为小写字母开头的 2~48 位小写字母、数字或下划线")
    private String code;

    private Integer sort;

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 255)
    private String remark;
}

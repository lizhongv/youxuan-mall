package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增 / 修改菜单 */
@Data
public class MenuSaveRequest {

    /** 为空或 0 表示顶级 */
    private Long parentId;

    @NotBlank
    @Pattern(regexp = "^(DIR|MENU|BUTTON)$", message = "类型只能是 DIR / MENU / BUTTON")
    private String type;

    @NotBlank
    @Size(max = 64)
    private String name;

    @Size(max = 128)
    private String path;

    @Size(max = 128)
    private String component;

    @Size(max = 64)
    private String icon;

    @Size(max = 128)
    @Pattern(regexp = "^$|^[a-z][a-z0-9_-]*(:[a-z][a-z0-9_-]*)+$", message = "权限码格式如 goods:goods:edit")
    private String permission;

    private Integer sort;

    @Min(0)
    @Max(1)
    private Integer visible;

    @Min(0)
    @Max(1)
    private Integer status;
}

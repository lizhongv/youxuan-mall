package com.platform.cloud.mall.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 角色 */
@Data
public class RoleVO {

    private Long id;
    private String name;
    private String code;
    private Integer sort;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}

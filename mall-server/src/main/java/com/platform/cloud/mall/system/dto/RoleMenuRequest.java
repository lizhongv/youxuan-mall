package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 角色授权：前端树组件的全选 + 半选节点 */
@Data
public class RoleMenuRequest {

    @NotNull
    private List<Long> menuIds;
}

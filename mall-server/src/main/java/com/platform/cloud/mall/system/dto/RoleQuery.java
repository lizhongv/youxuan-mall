package com.platform.cloud.mall.system.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色分页：keyword 匹配名称 / 编码 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleQuery extends PageQuery {

    private Integer status;
}

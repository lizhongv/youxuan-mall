package com.platform.cloud.mall.system.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 管理员分页：keyword 匹配用户名 / 昵称 / 手机号 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminQuery extends PageQuery {

    private Integer status;
}

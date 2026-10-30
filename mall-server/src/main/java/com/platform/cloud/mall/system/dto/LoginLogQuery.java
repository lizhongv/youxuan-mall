package com.platform.cloud.mall.system.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 登录日志筛选 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends PageQuery {

    private String userType;
    private String username;
    private Integer success;
}

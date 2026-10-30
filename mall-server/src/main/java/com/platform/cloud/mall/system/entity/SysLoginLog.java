package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 登录日志（管理员与会员共用，按 userType 区分） */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_login_log")
public class SysLoginLog extends BaseEntity {

    private String userType;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private Integer success;
    private String message;
}

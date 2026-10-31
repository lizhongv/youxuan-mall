package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 后台管理员 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_admin")
public class SysAdmin extends BaseEntity {

    private String username;
    /** BCrypt 密文，任何出参都不能带出 */
    private String password;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private String lastLoginIp;
    private String remark;
    /** 1 = 下次登录后必须先改密码（初始账号、被他人新建或重置密码的账号） */
    private Integer mustChangePassword;
    private LocalDateTime pwdChangedAt;
    /** 令牌版本号：改密 / 停用 / 调整角色时自增，旧令牌全部失效；只能用 SQL 自增，不能整行覆盖 */
    private Integer tokenVersion;
}

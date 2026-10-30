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
}

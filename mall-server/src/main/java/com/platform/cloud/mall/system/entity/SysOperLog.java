package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 后台操作日志 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    private Long adminId;
    private String username;
    private String module;
    private String action;
    private String method;
    private String uri;
    private String params;
    private String ip;
    private Integer success;
    private String errorMsg;
    private Integer costMs;
}

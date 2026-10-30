package com.platform.cloud.mall.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 登录日志 */
@Data
public class LoginLogVO {

    private Long id;
    private String userType;
    private String username;
    private String ip;
    private String userAgent;
    private Integer success;
    private String message;
    private LocalDateTime createdAt;
}

package com.platform.cloud.mall.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志 */
@Data
public class OperLogVO {

    private Long id;
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
    private LocalDateTime createdAt;
}

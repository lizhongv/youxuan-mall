package com.platform.cloud.mall.system.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 管理员 */
@Data
public class AdminVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String mobile;
    private String email;
    private Integer status;
    private List<Long> roleIds;
    private List<String> roleNames;
    private LocalDateTime lastLoginAt;
    private String remark;
    private LocalDateTime createdAt;
}

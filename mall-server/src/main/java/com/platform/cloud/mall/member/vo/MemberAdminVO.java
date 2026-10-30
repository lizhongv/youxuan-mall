package com.platform.cloud.mall.member.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 后台会员列表 / 详情；addresses 仅详情返回 */
@Data
public class MemberAdminVO {

    private Long id;
    /** 已脱敏 */
    private String mobile;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private Long levelId;
    private String levelName;
    private Integer points;
    private Integer growth;
    private BigDecimal balance;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private List<AddressVO> addresses;
}

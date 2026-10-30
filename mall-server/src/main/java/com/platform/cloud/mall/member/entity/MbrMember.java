package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 会员。余额、积分、成长值只能通过 MemberMapper 的条件更新修改，不能 updateById 整行覆盖，否则并发下会丢更新。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_member")
public class MbrMember extends BaseEntity {

    private String mobile;
    /** BCrypt 密文；验证码注册的会员为空串 */
    private String password;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private Long levelId;
    private Integer points;
    private Integer growth;
    private BigDecimal balance;
    private Integer status;
    private String registerIp;
    private LocalDateTime lastLoginAt;
}

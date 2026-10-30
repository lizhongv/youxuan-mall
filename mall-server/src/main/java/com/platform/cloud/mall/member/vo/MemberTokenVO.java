package com.platform.cloud.mall.member.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 会员登录 / 刷新结果。expiresIn 为 accessToken 有效秒数 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberTokenVO {

    private String accessToken;
    private String refreshToken;
    private Integer expiresIn;
}

package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 登出。refreshToken 可空：带上时一并作废，防止登出后仍能用它换新令牌 */
@Data
public class MemberLogoutRequest {

    @Size(max = 2048)
    private String refreshToken;
}

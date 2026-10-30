package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 刷新令牌 */
@Data
public class MemberRefreshRequest {

    @NotBlank
    private String refreshToken;
}

package com.platform.cloud.mall.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 会员修改资料，字段为空表示不修改 */
@Data
public class MemberProfileRequest {

    @Size(min = 1, max = 32, message = "昵称长度需为 1~32 个字符")
    private String nickname;

    @Size(max = 255)
    private String avatar;

    /** 0 未知 1 男 2 女 */
    @Min(0)
    @Max(2)
    private Integer gender;

    /** yyyy-MM-dd */
    @Past(message = "生日必须早于今天")
    private LocalDate birthday;
}

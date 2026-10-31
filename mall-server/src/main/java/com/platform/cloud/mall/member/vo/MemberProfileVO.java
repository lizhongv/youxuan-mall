package com.platform.cloud.mall.member.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** 会员中心首页。计数用 Integer：全局把 Long 序列化成字符串，计数需要输出数字 */
@Data
public class MemberProfileVO {

    private Long id;
    /** 已脱敏 */
    private String mobile;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private String levelName;
    private BigDecimal discount;
    private Integer points;
    private Integer growth;
    private BigDecimal balance;
    private Integer couponCount;
    private Integer favoriteCount;
    /** PENDING_PAY / PENDING_DELIVERY / DELIVERED / TO_REVIEW / AFTERSALE */
    private Map<String, Integer> orderCount;
    /** 是否已设置登录密码，前端据此决定修改密码页是否要求输入原密码 */
    private Boolean hasPassword;
    /** 是否已设置余额支付密码，前端据此在余额支付前引导设置 */
    private Boolean hasPayPassword;
}

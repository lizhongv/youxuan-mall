package com.platform.cloud.mall.member.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 会员第三方账号绑定（mbr_social）。
 * <p>
 * 一个会员在同一平台只绑定一个账号，一个第三方账号只属于一个会员，两条唯一索引共同保证；
 * 公众号 openid 只在该公众号下唯一，所以 platform 区分到「哪个应用」而不是笼统的「微信」。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mbr_social")
public class MbrSocial extends BaseEntity {

    private Long memberId;
    /** 见 MemberApi.PLATFORM_WECHAT_MP */
    private String platform;
    private String openid;
    private String unionid;
}

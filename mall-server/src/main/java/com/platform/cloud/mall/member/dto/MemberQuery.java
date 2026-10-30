package com.platform.cloud.mall.member.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 后台会员分页：keyword 匹配手机号 / 昵称 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberQuery extends PageQuery {

    private Long levelId;
    private Integer status;
}

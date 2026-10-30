package com.platform.cloud.mall.member.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 账户流水筛选。商城端忽略 memberId，强制使用当前登录会员 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AccountLogQuery extends PageQuery {

    private Long memberId;

    /** BALANCE / POINTS / GROWTH */
    private String accountType;
}

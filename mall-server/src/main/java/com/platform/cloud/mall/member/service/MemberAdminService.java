package com.platform.cloud.mall.member.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.member.dto.AdjustRequest;
import com.platform.cloud.mall.member.dto.MemberQuery;
import com.platform.cloud.mall.member.vo.MemberAdminVO;

/**
 * 后台会员管理。
 */
public interface MemberAdminService {

    PageVO<MemberAdminVO> page(MemberQuery query);

    MemberAdminVO detail(Long id);

    /** 1 正常 0 冻结；冻结后不能登录和刷新令牌 */
    void updateStatus(Long id, Integer status);

    /** 调整余额或积分，写 ADMIN_ADJUST 流水 */
    void adjust(Long id, AdjustRequest req);
}

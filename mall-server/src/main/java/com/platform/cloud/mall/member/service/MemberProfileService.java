package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.dto.MemberPasswordRequest;
import com.platform.cloud.mall.member.dto.MemberProfileRequest;
import com.platform.cloud.mall.member.vo.MemberProfileVO;
import com.platform.cloud.mall.member.vo.MemberTokenVO;

/**
 * 会员中心个人资料。
 */
public interface MemberProfileService {

    MemberProfileVO profile(Long memberId);

    void updateProfile(Long memberId, MemberProfileRequest req);

    /** 修改密码：作废该会员所有旧令牌，并为当前会话换发新令牌 */
    MemberTokenVO changePassword(Long memberId, MemberPasswordRequest req);
}

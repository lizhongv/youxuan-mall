package com.platform.cloud.mall.member.service;

import com.platform.cloud.mall.member.dto.MemberPasswordRequest;
import com.platform.cloud.mall.member.dto.MemberProfileRequest;
import com.platform.cloud.mall.member.vo.MemberProfileVO;

/**
 * 会员中心个人资料。
 */
public interface MemberProfileService {

    MemberProfileVO profile(Long memberId);

    void updateProfile(Long memberId, MemberProfileRequest req);

    void changePassword(Long memberId, MemberPasswordRequest req);
}

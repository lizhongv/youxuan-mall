package com.platform.cloud.mall.member.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.mall.common.security.TokenVersionSource;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.member.entity.MbrMember;
import com.platform.cloud.mall.member.mapper.MbrMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 会员令牌版本号的数据库来源。
 */
@Component
@RequiredArgsConstructor
public class MemberTokenVersionSource implements TokenVersionSource {

    private final MbrMemberMapper memberMapper;

    @Override
    public UserType userType() {
        return UserType.MEMBER;
    }

    @Override
    public Integer loadVersion(Long userId) {
        MbrMember m = memberMapper.selectOne(new LambdaQueryWrapper<MbrMember>()
                .select(MbrMember::getId, MbrMember::getTokenVersion)
                .eq(MbrMember::getId, userId));
        if (m == null) {
            return null;
        }
        return m.getTokenVersion() == null ? 0 : m.getTokenVersion();
    }

    @Override
    public void increment(Long userId) {
        // SQL 自增：并发作废不会互相覆盖
        memberMapper.update(null, new LambdaUpdateWrapper<MbrMember>()
                .setSql("token_version = token_version + 1")
                .eq(MbrMember::getId, userId));
    }
}

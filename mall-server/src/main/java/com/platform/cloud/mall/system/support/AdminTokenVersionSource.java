package com.platform.cloud.mall.system.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.mall.common.security.TokenVersionSource;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 管理员令牌版本号的数据库来源。停用的管理员同样返回版本号：停用时已自增过版本，旧令牌自然失效；
 * 新令牌在登录 / 刷新时就被拒绝签发。
 */
@Component
@RequiredArgsConstructor
public class AdminTokenVersionSource implements TokenVersionSource {

    private final SysAdminMapper adminMapper;

    @Override
    public UserType userType() {
        return UserType.ADMIN;
    }

    @Override
    public Integer loadVersion(Long userId) {
        SysAdmin a = adminMapper.selectOne(new LambdaQueryWrapper<SysAdmin>()
                .select(SysAdmin::getId, SysAdmin::getTokenVersion)
                .eq(SysAdmin::getId, userId));
        if (a == null) {
            return null;
        }
        return a.getTokenVersion() == null ? 0 : a.getTokenVersion();
    }

    @Override
    public void increment(Long userId) {
        // 用 SQL 自增而不是读出来 +1 再写回：并发的两次作废不会互相覆盖成同一个值
        adminMapper.update(null, new LambdaUpdateWrapper<SysAdmin>()
                .setSql("token_version = token_version + 1")
                .eq(SysAdmin::getId, userId));
    }
}

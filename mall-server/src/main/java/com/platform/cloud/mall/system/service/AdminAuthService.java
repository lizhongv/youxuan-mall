package com.platform.cloud.mall.system.service;

import com.platform.cloud.mall.system.dto.AdminLoginRequest;
import com.platform.cloud.mall.system.dto.AdminProfileRequest;
import com.platform.cloud.mall.system.dto.ChangePasswordRequest;
import com.platform.cloud.mall.system.vo.AdminInfoVO;
import com.platform.cloud.mall.system.vo.TokenVO;

/**
 * 管理员认证与个人中心。
 */
public interface AdminAuthService {

    /**
     * 用户名密码登录。同一账号在同一 IP 连续失败 2 次后需要图形验证码，5 次锁定该 IP 15 分钟；
     * 同一 IP 每分钟最多尝试 20 次。见 LoginAttemptGuard。
     */
    TokenVO login(AdminLoginRequest req);

    /** 用 refresh 令牌换新令牌对；会重新从库里读取角色权限，旧 refresh 令牌原子作废（只能用一次） */
    TokenVO refresh(String refreshToken);

    /** 登出：access 与 refresh 令牌的 jti 都拉黑到过期为止；refresh 由前端随登出请求提交，可为空 */
    void logout(String accessToken, String refreshToken);

    AdminInfoVO me(Long adminId);

    void updateProfile(Long adminId, AdminProfileRequest req);

    /** 修改密码：清除强制改密标记，作废该账号所有旧令牌，并为当前会话换发新令牌 */
    TokenVO changePassword(Long adminId, ChangePasswordRequest req);
}

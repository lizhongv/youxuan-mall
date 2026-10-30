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

    /** 用户名密码登录；连续失败 5 次锁定 15 分钟 */
    TokenVO login(AdminLoginRequest req);

    /** 用 refresh 令牌换新令牌对；会重新从库里读取角色权限，旧 refresh 令牌作废 */
    TokenVO refresh(String refreshToken);

    /** 登出：把 access 令牌的 jti 拉黑到过期为止 */
    void logout(String accessToken);

    AdminInfoVO me(Long adminId);

    void updateProfile(Long adminId, AdminProfileRequest req);

    void changePassword(Long adminId, ChangePasswordRequest req);
}

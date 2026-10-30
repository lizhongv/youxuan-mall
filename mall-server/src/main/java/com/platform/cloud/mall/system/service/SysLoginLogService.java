package com.platform.cloud.mall.system.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.common.security.UserType;
import com.platform.cloud.mall.system.dto.LoginLogQuery;
import com.platform.cloud.mall.system.vo.LoginLogVO;

/**
 * 登录日志，管理员与会员共用。
 */
public interface SysLoginLogService {

    PageVO<LoginLogVO> page(LoginLogQuery query);

    /**
     * 记录一次登录尝试。必须在请求线程调用（要读取 IP 与 User-Agent），落库异步进行。
     *
     * @param userId 登录失败且账号不存在时为 null
     */
    void record(UserType userType, Long userId, String username, boolean success, String message);
}

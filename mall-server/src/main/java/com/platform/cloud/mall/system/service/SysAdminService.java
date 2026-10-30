package com.platform.cloud.mall.system.service;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.mall.system.dto.AdminQuery;
import com.platform.cloud.mall.system.dto.AdminSaveRequest;
import com.platform.cloud.mall.system.vo.AdminVO;

/**
 * 管理员维护。内置 admin 与当前登录账号不能被停用或删除，防止把自己锁在系统外。
 */
public interface SysAdminService {

    PageVO<AdminVO> page(AdminQuery query);

    AdminVO detail(Long id);

    Long create(AdminSaveRequest req);

    void update(Long id, AdminSaveRequest req);

    void delete(Long id);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String password);
}

package com.platform.cloud.mall.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.platform.cloud.mall.common.config.MallProperties;
import com.platform.cloud.mall.system.entity.SysAdmin;
import com.platform.cloud.mall.system.entity.SysAdminRole;
import com.platform.cloud.mall.system.entity.SysRole;
import com.platform.cloud.mall.system.mapper.SysAdminMapper;
import com.platform.cloud.mall.system.mapper.SysAdminRoleMapper;
import com.platform.cloud.mall.system.mapper.SysRoleMapper;
import com.platform.cloud.mall.system.support.SystemConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 首次启动创建内置超级管理员 admin。
 * <p>
 * 密码哈希不写进种子 SQL：仓库里的哈希等于公开的默认口令，而这里从 {@code MALL_INIT_ADMIN_PASSWORD} 读取，
 * 部署时就能设成随机值。只在 admin 不存在时创建，已有账号的密码不会被覆盖。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    /** MallProperties 里的默认口令，仅用于提示修改 */
    private static final String DEFAULT_PASSWORD = "admin123";

    private final SysAdminMapper adminMapper;
    private final SysRoleMapper roleMapper;
    private final SysAdminRoleMapper adminRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final MallProperties props;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        Long exists = adminMapper.selectCount(new LambdaQueryWrapper<SysAdmin>()
                .eq(SysAdmin::getUsername, SystemConstants.ADMIN_USERNAME));
        if (exists != null && exists > 0) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> createAdmin());
            log.info("已创建内置超级管理员 admin");
            if (DEFAULT_PASSWORD.equals(props.getInitAdminPassword())) {
                log.warn("admin 使用默认初始密码，请登录后台立即修改，或部署时设置 MALL_INIT_ADMIN_PASSWORD");
            }
        } catch (DuplicateKeyException e) {
            // 多实例同时首次启动，另一个实例已经创建
            log.info("内置管理员已由其他实例创建");
        }
    }

    private void createAdmin() {
        SysRole superRole = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, SystemConstants.SUPER_ADMIN_CODE));
        if (superRole == null) {
            // 种子数据被误删时兜底，保证系统总有一个能登录的超管
            superRole = new SysRole();
            superRole.setName("超级管理员");
            superRole.setCode(SystemConstants.SUPER_ADMIN_CODE);
            superRole.setSort(0);
            superRole.setStatus(SystemConstants.ENABLED);
            superRole.setRemark("拥有全部权限");
            roleMapper.insert(superRole);
        }
        SysAdmin admin = new SysAdmin();
        admin.setUsername(SystemConstants.ADMIN_USERNAME);
        admin.setPassword(passwordEncoder.encode(props.getInitAdminPassword()));
        admin.setNickname("超级管理员");
        admin.setAvatar("");
        admin.setMobile("");
        admin.setEmail("");
        admin.setStatus(SystemConstants.ENABLED);
        admin.setLastLoginIp("");
        admin.setRemark("系统内置账号");
        adminMapper.insert(admin);
        adminRoleMapper.insert(new SysAdminRole(admin.getId(), superRole.getId()));
    }
}

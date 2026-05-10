package com.platform.cloud.bootstrap.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充 BaseEntity 上的 created_at / updated_at / created_by / updated_by。
 * <p>
 * Phase 2.0 起本类不直接读 SecurityContext（避免 starter-mybatis 依赖 spring-security）。
 * Phase 2 接入用户域 DB 后，会通过 Spring Bean 注入 {@code UserContextHolder} 取真实用户 id。
 * 当前所有写入统一标记为匿名用户。
 */
@Slf4j
@Component
public class AuditMetaHandler implements MetaObjectHandler {

    private static final Long ANONYMOUS_USER_ID = -1L;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, ANONYMOUS_USER_ID);
        strictInsertFill(metaObject, "updatedBy", Long.class, ANONYMOUS_USER_ID);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, ANONYMOUS_USER_ID);
    }
}

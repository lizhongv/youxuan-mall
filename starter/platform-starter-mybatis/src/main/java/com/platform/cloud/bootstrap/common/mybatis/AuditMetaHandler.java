package com.platform.cloud.bootstrap.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充 BaseEntity 上的 created_at / updated_at / created_by / updated_by。
 * <p>
 * 操作人取自可选的 {@link CurrentUserIdProvider}；没有登录上下文时记为匿名（-1），便于和真实用户区分。
 */
@Component
@RequiredArgsConstructor
public class AuditMetaHandler implements MetaObjectHandler {

    private static final Long ANONYMOUS_USER_ID = -1L;

    private final ObjectProvider<CurrentUserIdProvider> userIdProvider;

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, userId);
        strictInsertFill(metaObject, "updatedBy", Long.class, userId);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // 更新时间和操作人每次都要覆盖，strictUpdateFill 在字段已有值时不会覆盖，所以直接 setValue
        if (metaObject.hasSetter("updatedAt")) {
            metaObject.setValue("updatedAt", LocalDateTime.now());
        }
        if (metaObject.hasSetter("updatedBy")) {
            metaObject.setValue("updatedBy", currentUserId());
        }
    }

    private Long currentUserId() {
        CurrentUserIdProvider provider = userIdProvider.getIfAvailable();
        Long id = provider == null ? null : provider.currentUserId();
        return id == null ? ANONYMOUS_USER_ID : id;
    }
}

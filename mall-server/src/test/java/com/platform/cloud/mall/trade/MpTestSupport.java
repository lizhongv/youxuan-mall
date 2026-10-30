package com.platform.cloud.mall.trade;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 单测里使用 LambdaQueryWrapper 需要 MyBatis-Plus 的实体元数据缓存（正常由 Spring 启动时建立），
 * 这里手工初始化，避免为了单测启动 Spring 容器。
 */
public final class MpTestSupport {

    private static final MapperBuilderAssistant ASSISTANT =
            new MapperBuilderAssistant(new MybatisConfiguration(), "");

    private MpTestSupport() {
    }

    public static synchronized void init(Class<?>... entities) {
        for (Class<?> c : entities) {
            if (TableInfoHelper.getTableInfo(c) == null) {
                TableInfoHelper.initTableInfo(ASSISTANT, c);
            }
        }
    }
}

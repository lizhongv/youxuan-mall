package com.platform.cloud.mall.testsupport;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;

/**
 * 不启动 Spring 时，Lambda Wrapper 解析列名需要的表元数据由这里手动注册。
 */
public final class TableInfoTestSupport {

    private static final MybatisConfiguration CONFIGURATION = new MybatisConfiguration();

    private TableInfoTestSupport() {
    }

    public static synchronized void init(Class<?>... entities) {
        for (Class<?> entity : entities) {
            if (TableInfoHelper.getTableInfo(entity) == null) {
                TableInfoHelper.initTableInfo(new MapperBuilderAssistant(CONFIGURATION, ""), entity);
            }
        }
    }
}

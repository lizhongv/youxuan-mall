package com.platform.cloud.mall.system.api;

import java.math.BigDecimal;

/**
 * 运营参数（sys_config）读取。实现带本地缓存，后台修改后立即刷新。
 * <p>
 * 已约定的键见 {@link ConfigKeys}。
 */
public interface ConfigApi {

    String getString(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    BigDecimal getDecimal(String key, BigDecimal defaultValue);
}

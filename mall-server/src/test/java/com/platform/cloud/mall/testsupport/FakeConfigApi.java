package com.platform.cloud.mall.testsupport;

import com.platform.cloud.mall.system.api.ConfigApi;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 内存版系统参数，单测里模拟「后台改参数」只需 put，比 mock 每个 getString 调用更直观。
 */
public class FakeConfigApi implements ConfigApi {

    private final Map<String, String> values = new HashMap<>();

    public FakeConfigApi put(String key, String value) {
        values.put(key, value);
        return this;
    }

    @Override
    public String getString(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = values.get(key);
        return v == null ? defaultValue : Integer.parseInt(v);
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String v = values.get(key);
        return v == null ? defaultValue : new BigDecimal(v);
    }
}

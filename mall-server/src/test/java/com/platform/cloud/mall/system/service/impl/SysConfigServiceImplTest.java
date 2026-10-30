package com.platform.cloud.mall.system.service.impl;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.api.ConfigKeys;
import com.platform.cloud.mall.system.entity.SysConfig;
import com.platform.cloud.mall.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 系统参数：类型校验与本地缓存。
 */
class SysConfigServiceImplTest {

    @Test
    void numericKeysAreValidated() {
        assertThatThrownBy(() -> SysConfigServiceImpl.validate(cfg(ConfigKeys.ORDER_PAY_TIMEOUT_MINUTES), "0"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SysConfigServiceImpl.validate(cfg(ConfigKeys.POINTS_PER_YUAN), "1.5"))
                .hasMessageContaining("整数");
        assertThatThrownBy(() -> SysConfigServiceImpl.validate(cfg(ConfigKeys.FREIGHT_AMOUNT), "-1"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SysConfigServiceImpl.validate(cfg(ConfigKeys.FREIGHT_AMOUNT), "1.001"))
                .isInstanceOf(BusinessException.class);
        SysConfigServiceImpl.validate(cfg(ConfigKeys.FREIGHT_AMOUNT), "10.00");
        SysConfigServiceImpl.validate(cfg(ConfigKeys.AFTERSALE_DAYS), "0");
        SysConfigServiceImpl.validate(cfg(ConfigKeys.MALL_NAME), "任意文本");
    }

    @Test
    void readsAreServedFromCacheAndFallBackToDefaults() {
        SysConfigMapper mapper = mock(SysConfigMapper.class);
        SysConfig freight = cfg(ConfigKeys.FREIGHT_AMOUNT);
        freight.setConfigValue("10.00");
        SysConfig bad = cfg(ConfigKeys.ORDER_PAY_TIMEOUT_MINUTES);
        bad.setConfigValue("abc");
        when(mapper.selectList(any())).thenReturn(List.of(freight, bad));
        SysConfigServiceImpl service = new SysConfigServiceImpl(mapper);

        assertThat(service.getDecimal(ConfigKeys.FREIGHT_AMOUNT, BigDecimal.ZERO)).isEqualByComparingTo("10");
        assertThat(service.getInt(ConfigKeys.ORDER_PAY_TIMEOUT_MINUTES, 30)).isEqualTo(30);
        assertThat(service.getString("missing", "def")).isEqualTo("def");
        verify(mapper, times(1)).selectList(any());
    }

    private static SysConfig cfg(String key) {
        SysConfig c = new SysConfig();
        c.setId(1L);
        c.setConfigKey(key);
        c.setName(key);
        return c;
    }
}

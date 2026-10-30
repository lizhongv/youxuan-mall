package com.platform.cloud.mall.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.ConfigKeys;
import com.platform.cloud.mall.system.dto.ConfigBatchRequest;
import com.platform.cloud.mall.system.entity.SysConfig;
import com.platform.cloud.mall.system.mapper.SysConfigMapper;
import com.platform.cloud.mall.system.service.SysConfigService;
import com.platform.cloud.mall.system.vo.ConfigVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 系统参数，同时是 {@link ConfigApi} 的实现。
 * <p>
 * 参数在下单、算运费时高频读取，整表缓存在本地内存：本节点修改后提交即刷新；
 * 多实例部署时其他节点靠 {@link #CACHE_TTL_MILLIS} 过期重载，最多延迟 1 分钟生效，对运营参数可以接受。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService, ConfigApi {

    /** 本地缓存过期时间 */
    static final long CACHE_TTL_MILLIS = 60_000L;

    /** 必须是非负整数的键 */
    private static final Set<String> NON_NEGATIVE_INT_KEYS = Set.of(
            ConfigKeys.AFTERSALE_DAYS, ConfigKeys.POINTS_PER_YUAN, ConfigKeys.GROWTH_PER_YUAN);
    /** 必须是正整数的键：为 0 会导致订单立即关闭或立即收货 */
    private static final Set<String> POSITIVE_INT_KEYS = Set.of(
            ConfigKeys.ORDER_PAY_TIMEOUT_MINUTES, ConfigKeys.ORDER_AUTO_RECEIVE_DAYS);
    /** 必须是非负金额的键 */
    private static final Set<String> MONEY_KEYS = Set.of(
            ConfigKeys.FREIGHT_AMOUNT, ConfigKeys.FREIGHT_FREE_THRESHOLD, ConfigKeys.REGISTER_GIFT_BALANCE);

    private final SysConfigMapper configMapper;

    private volatile Map<String, String> cache;
    private volatile long loadedAt;

    @Override
    public List<ConfigVO> list() {
        return configMapper.selectList(new LambdaQueryWrapper<SysConfig>().orderByAsc(SysConfig::getConfigKey))
                .stream().map(SysConfigServiceImpl::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdate(List<ConfigBatchRequest.Item> items) {
        Map<String, SysConfig> existing = configMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, Function.identity()));
        for (ConfigBatchRequest.Item item : items) {
            SysConfig cfg = existing.get(item.getConfigKey());
            if (cfg == null) {
                throw new BusinessException("参数不存在：" + item.getConfigKey());
            }
            String value = item.getConfigValue().trim();
            validate(cfg, value);
            configMapper.update(null, new LambdaUpdateWrapper<SysConfig>()
                    .set(SysConfig::getConfigValue, value)
                    .eq(SysConfig::getId, cfg.getId()));
        }
        refreshAfterCommit();
    }

    @Override
    public String getString(String key, String defaultValue) {
        String v = snapshot().get(key);
        return v == null ? defaultValue : v;
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String v = snapshot().get(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 不是整数：{}，使用默认值 {}", key, v, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String v = snapshot().get(key);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(v.trim());
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 不是数字：{}，使用默认值 {}", key, v, defaultValue);
            return defaultValue;
        }
    }

    /** 立即重载缓存 */
    public void refresh() {
        Map<String, String> map = new HashMap<>();
        for (SysConfig c : configMapper.selectList(null)) {
            map.put(c.getConfigKey(), c.getConfigValue());
        }
        cache = Map.copyOf(map);
        loadedAt = System.currentTimeMillis();
    }

    private Map<String, String> snapshot() {
        Map<String, String> c = cache;
        if (c == null || System.currentTimeMillis() - loadedAt > CACHE_TTL_MILLIS) {
            synchronized (this) {
                if (cache == null || System.currentTimeMillis() - loadedAt > CACHE_TTL_MILLIS) {
                    refresh();
                }
                c = cache;
            }
        }
        return c;
    }

    /** 事务提交后再刷新，避免回滚时缓存里留下未生效的值 */
    private void refreshAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    refresh();
                }
            });
        } else {
            refresh();
        }
    }

    static void validate(SysConfig cfg, String value) {
        String key = cfg.getConfigKey();
        String label = cfg.getName() == null || cfg.getName().isBlank() ? key : cfg.getName();
        if (POSITIVE_INT_KEYS.contains(key) || NON_NEGATIVE_INT_KEYS.contains(key)) {
            int min = POSITIVE_INT_KEYS.contains(key) ? 1 : 0;
            try {
                if (Integer.parseInt(value) < min) {
                    throw new BusinessException(label + "不能小于 " + min);
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(label + "必须是整数");
            }
        } else if (MONEY_KEYS.contains(key)) {
            try {
                BigDecimal d = new BigDecimal(value);
                if (d.signum() < 0 || d.scale() > 2) {
                    throw new BusinessException(label + "必须是不小于 0 且最多两位小数的金额");
                }
            } catch (NumberFormatException e) {
                throw new BusinessException(label + "必须是金额数字");
            }
        }
    }

    private static ConfigVO toVO(SysConfig c) {
        ConfigVO vo = new ConfigVO();
        vo.setId(c.getId());
        vo.setConfigKey(c.getConfigKey());
        vo.setConfigValue(c.getConfigValue());
        vo.setName(c.getName());
        vo.setRemark(c.getRemark());
        return vo;
    }
}

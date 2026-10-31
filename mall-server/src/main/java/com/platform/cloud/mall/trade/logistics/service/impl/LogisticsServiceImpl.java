package com.platform.cloud.mall.trade.logistics.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.mall.system.api.ConfigApi;
import com.platform.cloud.mall.system.api.InfraConfigKeys;
import com.platform.cloud.mall.trade.logistics.service.LogisticsService;
import com.platform.cloud.mall.trade.logistics.support.ExpressCompany;
import com.platform.cloud.mall.trade.logistics.support.ExpressCompanyResolver;
import com.platform.cloud.mall.trade.logistics.support.Kuaidi100Client;
import com.platform.cloud.mall.trade.logistics.support.LogisticsState;
import com.platform.cloud.mall.trade.logistics.vo.LogisticsVO;
import com.platform.cloud.mall.trade.order.entity.Order;
import com.platform.cloud.mall.trade.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

/**
 * 物流查询：订单 → 解析快递公司 → 读缓存 → 调快递100 → 写缓存。
 * <p>
 * 快递100 按查询次数计费且有并发限制，订单详情页又会被反复打开，所以结果缓存在 Redis：
 * 在途 30 分钟（轨迹更新频率通常是小时级），已签收 1 天（不会再变），查询无结果 10 分钟（刚发货未揽收，稍后再查）。
 * 其他失败（签名错误、余额不足、网络异常）不缓存，修复配置后立即可查。
 * 任何失败都返回带提示的空轨迹，绝不抛异常影响订单详情页。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    static final String CACHE_PREFIX = "mall:logistics:";
    static final Duration TTL_IN_TRANSIT = Duration.ofMinutes(30);
    static final Duration TTL_SIGNED = Duration.ofDays(1);
    static final Duration TTL_NO_RESULT = Duration.ofMinutes(10);

    static final String PROVIDER_KUAIDI100 = "KUAIDI100";
    static final String MSG_NOT_ENABLED = "未开通物流查询";
    static final String MSG_NOT_DELIVERED = "订单尚未发货";
    static final String MSG_UNSUPPORTED = "暂不支持查询该快递公司的物流";
    static final String MSG_NO_RESULT = "暂无物流轨迹，快递公司可能尚未揽收，请稍后再查";
    static final String MSG_FAILED = "物流信息暂时无法查询，请稍后再试";

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final OrderMapper orderMapper;
    private final ConfigApi config;
    private final ExpressCompanyResolver resolver;
    private final Kuaidi100Client kuaidi100;
    private final StringRedisTemplate redis;

    @Override
    public LogisticsVO memberLogistics(Long memberId, Long orderId) {
        Order order = orderMapper.selectOne(Wrappers.<Order>lambdaQuery()
                .eq(Order::getId, orderId)
                .eq(Order::getMemberId, memberId));
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return query(order);
    }

    @Override
    public LogisticsVO adminLogistics(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return query(order);
    }

    LogisticsVO query(Order order) {
        LogisticsVO vo = new LogisticsVO();
        vo.setCompanyName(order.getDeliveryCompany());
        vo.setTrackingNo(order.getDeliveryNo());
        setState(vo, LogisticsState.NONE);
        if (isBlank(order.getDeliveryNo())) {
            vo.setMessage(MSG_NOT_DELIVERED);
            return vo;
        }
        Optional<ExpressCompany> company = resolver.resolve(order.getDeliveryCompany());
        company.ifPresent(c -> {
            vo.setCompany(c.code());
            vo.setCompanyName(c.name());
        });

        String provider = config.getString(InfraConfigKeys.LOGISTICS_PROVIDER, "NONE").trim().toUpperCase(Locale.ROOT);
        String customer = config.getString(InfraConfigKeys.LOGISTICS_KUAIDI100_CUSTOMER, "").trim();
        String key = config.getString(InfraConfigKeys.LOGISTICS_KUAIDI100_KEY, "").trim();
        if (!PROVIDER_KUAIDI100.equals(provider) || customer.isEmpty() || key.isEmpty()) {
            vo.setMessage(MSG_NOT_ENABLED);
            return vo;
        }
        if (company.isEmpty()) {
            vo.setMessage(MSG_UNSUPPORTED);
            return vo;
        }

        String trackingNo = order.getDeliveryNo().trim();
        String cacheKey = CACHE_PREFIX + company.get().code() + ":" + trackingNo;
        LogisticsVO cached = readCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        String phone = company.get().requiresPhone() ? lastFour(order.getReceiverMobile()) : null;
        Kuaidi100Client.Result r = kuaidi100.query(customer, key, company.get().code(), trackingNo, phone);
        if (r.ok()) {
            setState(vo, r.state());
            vo.setTraces(new ArrayList<>(r.traces()));
            writeCache(cacheKey, vo, r.signed() ? TTL_SIGNED : TTL_IN_TRANSIT);
        } else if (Kuaidi100Client.NO_RESULT.equals(r.errorCode())) {
            vo.setMessage(MSG_NO_RESULT);
            writeCache(cacheKey, vo, TTL_NO_RESULT);
        } else {
            vo.setMessage(MSG_FAILED);
        }
        return vo;
    }

    private static void setState(LogisticsVO vo, LogisticsState state) {
        vo.setState(state.name());
        vo.setStateText(state.text());
    }

    /** 顺丰只需要手机号后四位做校验，传后四位比传完整号码泄露更少 */
    static String lastFour(String mobile) {
        if (mobile == null) {
            return null;
        }
        String digits = mobile.replaceAll("\\D", "");
        return digits.length() < 4 ? null : digits.substring(digits.length() - 4);
    }

    /** Redis 不可用时退化为每次直接查询，不影响功能 */
    private LogisticsVO readCache(String key) {
        try {
            String json = redis.opsForValue().get(key);
            return json == null ? null : JSON.readValue(json, LogisticsVO.class);
        } catch (Exception e) {
            log.warn("读取物流缓存失败 key={} msg={}", key, e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, LogisticsVO vo, Duration ttl) {
        try {
            redis.opsForValue().set(key, JSON.writeValueAsString(vo), ttl);
        } catch (Exception e) {
            log.warn("写入物流缓存失败 key={} msg={}", key, e.getMessage());
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

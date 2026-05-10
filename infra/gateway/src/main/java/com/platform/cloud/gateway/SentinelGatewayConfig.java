package com.platform.cloud.gateway;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * 网关层 Sentinel：QPS 限流 + 自定义 R 格式响应。
 * <p>注意：Sentinel 1.8.8 自带的 {@code SentinelGatewayBlockExceptionHandler} 用了 Spring 5 的
 * {@code ServerResponse.status(HttpStatus)} API，在 Spring 6 下抛 NoSuchMethodError；
 * 故这里不注册它，改用一个高优先级的 {@link WebExceptionHandler} 自己写响应。
 */
@Slf4j
@Configuration
public class SentinelGatewayConfig {

    @Bean
    @Order(-1)
    public GlobalFilter sentinelGatewayFilter() {
        return new SentinelGatewayFilter();
    }

    /** 兼容 Spring 6 的 BlockException 处理器：直接写 DataBuffer 避开 ServerResponse 的 API 变化 */
    @Bean("platformGatewayBlockHandler")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public WebExceptionHandler platformGatewayBlockHandler() {
        return (exchange, ex) -> {
            if (!BlockException.isBlockException(ex)) {
                return Mono.error(ex);
            }
            String reason;
            if (ex instanceof FlowException)            reason = "网关限流: 请求过于频繁";
            else if (ex instanceof DegradeException)    reason = "网关熔断: 服务降级中";
            else if (ex instanceof ParamFlowException)  reason = "网关热点参数限流";
            else                                         reason = "网关拦截: " + ex.getClass().getSimpleName();

            log.warn("Gateway sentinel block: {} on {}", ex.getClass().getSimpleName(),
                    exchange.getRequest().getURI().getRawPath());

            ServerHttpResponse resp = exchange.getResponse();
            if (resp.isCommitted()) {
                return Mono.error(ex);
            }
            resp.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            resp.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            String body = String.format(
                    "{\"code\":42900,\"msg\":\"%s\",\"data\":null,\"ts\":%d}",
                    reason, System.currentTimeMillis());
            DataBuffer buf = resp.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
            return resp.writeWith(Mono.just(buf));
        };
    }

    @PostConstruct
    public void initRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("auth-route").setCount(2).setIntervalSec(1));
        rules.add(new GatewayFlowRule("goods-route").setCount(5).setIntervalSec(1));
        GatewayRuleManager.loadRules(rules);
        log.info("Gateway sentinel rules loaded: {} rules", rules.size());
    }
}

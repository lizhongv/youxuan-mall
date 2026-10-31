package com.platform.cloud.goods.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 2.4 演示：进程启动时载入 Sentinel 流控/熔断规则。
 * <p>真实环境会把规则放到 Nacos / Apollo 配置中心做动态推送，本阶段先硬编码。
 */
@Slf4j
@Component
public class SentinelRuleConfig {

    @PostConstruct
    public void init() {
        loadFlowRules();
        loadDegradeRules();
        log.info("Sentinel rules loaded: {} flow + {} degrade",
                FlowRuleManager.getRules().size(), DegradeRuleManager.getRules().size());
    }

    /** 流控规则（QPS）：Sentinel 自动用 URL 作为资源名（开箱即用） */
    private void loadFlowRules() {
        FlowRule whoami = new FlowRule();
        whoami.setResource("/api/goods/_internal/whoami");
        whoami.setGrade(RuleConstant.FLOW_GRADE_QPS);
        whoami.setCount(2);                               // 最大 2 QPS
        whoami.setControlBehavior(RuleConstant.CONTROL_BEHAVIOR_DEFAULT);

        FlowRule items = new FlowRule();
        items.setResource("/api/goods/items");
        items.setGrade(RuleConstant.FLOW_GRADE_QPS);
        items.setCount(10);                               // 列表查询限 10 QPS

        FlowRuleManager.loadRules(List.of(whoami, items));
    }

    /** 熔断规则：异常比例 50% 以上、时间窗口 10s、最少请求 5 个时熔断 30s */
    private void loadDegradeRules() {
        DegradeRule rule = new DegradeRule("/api/goods/_internal/whoami")
                .setGrade(CircuitBreakerStrategy.ERROR_RATIO.getType())
                .setCount(0.5)
                .setTimeWindow(30)
                .setMinRequestAmount(5)
                .setStatIntervalMs(10_000);
        DegradeRuleManager.loadRules(List.of(rule));
    }
}

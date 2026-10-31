package com.platform.cloud.mall.trade.logistics.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.logistics.service.LogisticsService;
import com.platform.cloud.mall.trade.logistics.vo.LogisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城端订单物流，只能查询当前会员自己的订单。
 */
@Tag(name = "商城-订单物流")
@RestController
@RequestMapping("/app/order")
@RequiredArgsConstructor
public class AppOrderLogisticsController {

    private final LogisticsService logisticsService;

    @Operation(summary = "订单物流轨迹（时间倒序；失败或未开通时 traces 为空并给出 message）")
    @GetMapping("/{id}/logistics")
    public R<LogisticsVO> logistics(@PathVariable Long id) {
        return R.ok(logisticsService.memberLogistics(SecurityUtils.currentUserId(), id));
    }
}

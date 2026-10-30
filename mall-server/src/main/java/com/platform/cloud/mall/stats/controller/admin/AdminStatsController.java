package com.platform.cloud.mall.stats.controller.admin;

import com.platform.cloud.bootstrap.common.exception.BusinessException;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.stats.service.StatsService;
import com.platform.cloud.mall.stats.vo.OrderStatusStatVO;
import com.platform.cloud.mall.stats.vo.OverviewVO;
import com.platform.cloud.mall.stats.vo.TopGoodsVO;
import com.platform.cloud.mall.stats.vo.TrendPointVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "管理端-数据统计")
@Validated
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @Operation(summary = "数据概览")
    @PreAuthorize("@ss.has('stats:view')")
    @GetMapping("/overview")
    public R<OverviewVO> overview() {
        return R.ok(statsService.overview());
    }

    @Operation(summary = "销售趋势（days = 7 或 30）")
    @PreAuthorize("@ss.has('stats:view')")
    @GetMapping("/trend")
    public R<List<TrendPointVO>> trend(@RequestParam(defaultValue = "7") int days) {
        if (days != 7 && days != 30) {
            throw new BusinessException("days 只支持 7 或 30");
        }
        return R.ok(statsService.trend(days));
    }

    @Operation(summary = "商品销量排行")
    @PreAuthorize("@ss.has('stats:view')")
    @GetMapping("/top-goods")
    public R<List<TopGoodsVO>> topGoods(@RequestParam(defaultValue = "30") @Min(1) @Max(365) int days,
                                        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return R.ok(statsService.topGoods(days, limit));
    }

    @Operation(summary = "订单状态分布")
    @PreAuthorize("@ss.has('stats:view')")
    @GetMapping("/order-status")
    public R<List<OrderStatusStatVO>> orderStatus() {
        return R.ok(statsService.orderStatus());
    }
}

package com.platform.cloud.mall.trade.review.controller.app;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.review.dto.ReviewCreateRequest;
import com.platform.cloud.mall.trade.review.service.ReviewService;
import com.platform.cloud.mall.trade.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "商城-评价")
@Validated
@RestController
@RequestMapping("/app/review")
@RequiredArgsConstructor
public class AppReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "发表评价（仅已完成订单，每条明细一次）")
    @PostMapping
    public R<Long> create(@Valid @RequestBody ReviewCreateRequest req) {
        return R.ok(reviewService.create(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "我的评价")
    @GetMapping("/mine")
    public R<PageVO<ReviewVO>> mine(@Valid PageQuery query) {
        return R.ok(reviewService.minePage(SecurityUtils.currentUserId(), query));
    }
}

package com.platform.cloud.mall.trade.review.controller.app;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.trade.review.dto.PublicReviewPageQuery;
import com.platform.cloud.mall.trade.review.service.ReviewService;
import com.platform.cloud.mall.trade.review.vo.ReviewStatVO;
import com.platform.cloud.mall.trade.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 商品评价公开接口（免登录），只返回显示中的评价 */
@Tag(name = "商城-公开评价")
@Validated
@RestController
@RequestMapping("/app/public/goods")
@RequiredArgsConstructor
public class PublicReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "商品评价分页")
    @GetMapping("/{id}/reviews")
    public R<PageVO<ReviewVO>> reviews(@PathVariable Long id, @Valid PublicReviewPageQuery query) {
        return R.ok(reviewService.publicPage(id, query));
    }

    @Operation(summary = "商品评价统计")
    @GetMapping("/{id}/review-stat")
    public R<ReviewStatVO> stat(@PathVariable Long id) {
        return R.ok(reviewService.stat(id));
    }
}

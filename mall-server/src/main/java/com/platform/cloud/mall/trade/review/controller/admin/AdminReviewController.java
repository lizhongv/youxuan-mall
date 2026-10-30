package com.platform.cloud.mall.trade.review.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.trade.review.dto.AdminReviewPageQuery;
import com.platform.cloud.mall.trade.review.dto.ReviewReplyRequest;
import com.platform.cloud.mall.trade.review.service.ReviewService;
import com.platform.cloud.mall.trade.review.vo.ReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理端-评价")
@Validated
@RestController
@RequestMapping("/admin/trade/review")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "评价分页")
    @PreAuthorize("@ss.has('trade:review:view')")
    @GetMapping("/page")
    public R<PageVO<ReviewVO>> page(@Valid AdminReviewPageQuery query) {
        return R.ok(reviewService.adminPage(query));
    }

    @Operation(summary = "商家回复")
    @PreAuthorize("@ss.has('trade:review:edit')")
    @PutMapping("/{id}/reply")
    public R<Void> reply(@PathVariable Long id, @Valid @RequestBody ReviewReplyRequest req) {
        reviewService.reply(SecurityUtils.currentUserId(), id, req.getReply());
        return R.ok();
    }

    @Operation(summary = "显示 / 隐藏")
    @PreAuthorize("@ss.has('trade:review:edit')")
    @PutMapping("/{id}/status")
    public R<Void> status(@PathVariable Long id,
                          @RequestParam @NotNull @Min(0) @Max(1) Integer status) {
        reviewService.updateStatus(SecurityUtils.currentUserId(), id, status);
        return R.ok();
    }
}

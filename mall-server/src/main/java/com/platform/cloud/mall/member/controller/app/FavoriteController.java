package com.platform.cloud.mall.member.controller.app;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.member.service.BrowseHistoryService;
import com.platform.cloud.mall.member.service.FavoriteService;
import com.platform.cloud.mall.member.vo.MemberGoodsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 收藏与足迹。
 */
@Tag(name = "收藏与足迹")
@RestController
@RequestMapping("/app/member")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final BrowseHistoryService historyService;

    @Operation(summary = "我的收藏")
    @GetMapping("/favorite/page")
    public R<PageVO<MemberGoodsVO>> favorites(@Valid PageQuery query) {
        return R.ok(favoriteService.page(SecurityUtils.currentUserId(), query));
    }

    @Operation(summary = "收藏商品")
    @PostMapping("/favorite/{goodsId}")
    public R<Void> addFavorite(@PathVariable Long goodsId) {
        favoriteService.add(SecurityUtils.currentUserId(), goodsId);
        return R.ok();
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorite/{goodsId}")
    public R<Void> removeFavorite(@PathVariable Long goodsId) {
        favoriteService.remove(SecurityUtils.currentUserId(), goodsId);
        return R.ok();
    }

    @Operation(summary = "我的足迹")
    @GetMapping("/history/page")
    public R<PageVO<MemberGoodsVO>> history(@Valid PageQuery query) {
        return R.ok(historyService.page(SecurityUtils.currentUserId(), query));
    }

    @Operation(summary = "清空足迹")
    @DeleteMapping("/history")
    public R<Void> clearHistory() {
        historyService.clear(SecurityUtils.currentUserId());
        return R.ok();
    }
}

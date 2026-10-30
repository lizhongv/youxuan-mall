package com.platform.cloud.mall.goods.controller.app;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.goods.dto.AppGoodsQuery;
import com.platform.cloud.mall.goods.service.AppGoodsService;
import com.platform.cloud.mall.goods.service.CategoryService;
import com.platform.cloud.mall.goods.vo.AppGoodsDetailVO;
import com.platform.cloud.mall.goods.vo.CategoryVO;
import com.platform.cloud.mall.goods.vo.GoodsItemVO;
import com.platform.cloud.mall.goods.vo.HomeVO;
import com.platform.cloud.mall.goods.vo.MallConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城端公开浏览（无需登录）。
 */
@Tag(name = "商城端-公开浏览")
@RestController
@RequestMapping("/app/public")
@RequiredArgsConstructor
public class PublicGoodsController {

    private final AppGoodsService appGoodsService;
    private final CategoryService categoryService;

    @Operation(summary = "首页聚合")
    @GetMapping("/home")
    public R<HomeVO> home() {
        return R.ok(appGoodsService.home());
    }

    @Operation(summary = "类目树（仅启用）")
    @GetMapping("/category/tree")
    public R<List<CategoryVO>> categoryTree() {
        return R.ok(categoryService.enabledTree());
    }

    @Operation(summary = "商品搜索 / 分页")
    @GetMapping("/goods/page")
    public R<PageVO<GoodsItemVO>> goodsPage(@Valid AppGoodsQuery q) {
        return R.ok(appGoodsService.page(q));
    }

    /** 已登录会员额外返回是否收藏，并记录足迹；管理员令牌访问时按游客处理 */
    @Operation(summary = "商品详情")
    @GetMapping("/goods/{id}")
    public R<AppGoodsDetailVO> goodsDetail(@PathVariable Long id) {
        Long memberId = SecurityUtils.isMember() ? SecurityUtils.currentUserIdOrNull() : null;
        return R.ok(appGoodsService.detail(id, memberId));
    }

    @Operation(summary = "商城公开参数")
    @GetMapping("/config")
    public R<MallConfigVO> config() {
        return R.ok(appGoodsService.mallConfig());
    }
}

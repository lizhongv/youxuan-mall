package com.platform.cloud.goods.search;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Goods Search", description = "ES 商品全文检索")
@Validated
@RestController
@RequestMapping("/api/goods/search")
@RequiredArgsConstructor
public class GoodsSearchController {

    private final GoodsSearchService service;

    @Operation(summary = "全文检索 + 分页")
    @GetMapping
    public R<PageVO<GoodsSearchDoc>> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return R.ok(service.search(q, page, size));
    }

    @Operation(summary = "[管理] 全量重建 ES 索引")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/reindex")
    public R<Integer> reindex() {
        return R.ok(service.reindexAll());
    }
}

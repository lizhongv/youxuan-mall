package com.platform.cloud.mall.trade.freight.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateQuery;
import com.platform.cloud.mall.trade.freight.dto.FreightTemplateRequest;
import com.platform.cloud.mall.trade.freight.service.FreightTemplateService;
import com.platform.cloud.mall.trade.freight.vo.FreightTemplateOptionVO;
import com.platform.cloud.mall.trade.freight.vo.FreightTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端：运费模板。
 */
@Tag(name = "管理端-运费模板")
@RestController
@RequestMapping("/admin/trade/freight-template")
@RequiredArgsConstructor
public class AdminFreightTemplateController {

    private final FreightTemplateService freightTemplateService;

    @Operation(summary = "运费模板分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('trade:freight:view')")
    public R<PageVO<FreightTemplateVO>> page(@Valid FreightTemplateQuery q) {
        return R.ok(freightTemplateService.page(q));
    }

    /** 商品编辑页也要选模板，因此商品查看权限同样可以访问下拉 */
    @Operation(summary = "启用中的运费模板下拉")
    @GetMapping("/list")
    @PreAuthorize("@ss.has('trade:freight:view') or @ss.has('goods:goods:view')")
    public R<List<FreightTemplateOptionVO>> list() {
        return R.ok(freightTemplateService.listOptions());
    }

    @Operation(summary = "运费模板详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('trade:freight:view')")
    public R<FreightTemplateVO> get(@PathVariable Long id) {
        return R.ok(freightTemplateService.get(id));
    }

    @Operation(summary = "新增运费模板")
    @PostMapping
    @PreAuthorize("@ss.has('trade:freight:edit')")
    public R<Long> create(@Valid @RequestBody FreightTemplateRequest req) {
        return R.ok(freightTemplateService.create(req));
    }

    @Operation(summary = "修改运费模板")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('trade:freight:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody FreightTemplateRequest req) {
        freightTemplateService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除运费模板")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('trade:freight:edit')")
    public R<Void> delete(@PathVariable Long id) {
        freightTemplateService.delete(id);
        return R.ok();
    }
}

package com.platform.cloud.mall.marketing.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.marketing.dto.SeckillQuery;
import com.platform.cloud.mall.marketing.dto.SeckillRequest;
import com.platform.cloud.mall.marketing.service.SeckillService;
import com.platform.cloud.mall.marketing.vo.SeckillVO;
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

/**
 * 管理端：秒杀活动。
 */
@Tag(name = "管理端-秒杀活动")
@RestController
@RequestMapping("/admin/marketing/seckill")
@RequiredArgsConstructor
public class SeckillAdminController {

    private final SeckillService seckillService;

    @Operation(summary = "秒杀活动分页")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('marketing:seckill:view')")
    public R<PageVO<SeckillVO>> page(@Valid SeckillQuery q) {
        return R.ok(seckillService.page(q));
    }

    @Operation(summary = "秒杀活动详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:seckill:view')")
    public R<SeckillVO> get(@PathVariable Long id) {
        return R.ok(seckillService.get(id));
    }

    @Operation(summary = "新增秒杀活动")
    @PostMapping
    @PreAuthorize("@ss.has('marketing:seckill:edit')")
    public R<Long> create(@Valid @RequestBody SeckillRequest req) {
        return R.ok(seckillService.create(req));
    }

    @Operation(summary = "修改秒杀活动")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:seckill:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody SeckillRequest req) {
        seckillService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除秒杀活动")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:seckill:edit')")
    public R<Void> delete(@PathVariable Long id) {
        seckillService.delete(id);
        return R.ok();
    }
}

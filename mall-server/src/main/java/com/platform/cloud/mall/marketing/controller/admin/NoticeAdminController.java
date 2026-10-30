package com.platform.cloud.mall.marketing.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.marketing.dto.NoticeQuery;
import com.platform.cloud.mall.marketing.dto.NoticeRequest;
import com.platform.cloud.mall.marketing.service.NoticeService;
import com.platform.cloud.mall.marketing.vo.NoticeVO;
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
 * 管理端：商城公告。
 */
@Tag(name = "管理端-商城公告")
@RestController
@RequestMapping("/admin/marketing/notice")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeService noticeService;

    @Operation(summary = "公告分页（不含正文）")
    @GetMapping("/page")
    @PreAuthorize("@ss.has('marketing:notice:view')")
    public R<PageVO<NoticeVO>> page(@Valid NoticeQuery q) {
        return R.ok(noticeService.page(q));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:notice:view')")
    public R<NoticeVO> get(@PathVariable Long id) {
        return R.ok(noticeService.get(id));
    }

    @Operation(summary = "新增公告")
    @PostMapping
    @PreAuthorize("@ss.has('marketing:notice:edit')")
    public R<Long> create(@Valid @RequestBody NoticeRequest req) {
        return R.ok(noticeService.create(req));
    }

    @Operation(summary = "修改公告")
    @PutMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:notice:edit')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeRequest req) {
        noticeService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("@ss.has('marketing:notice:edit')")
    public R<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return R.ok();
    }
}

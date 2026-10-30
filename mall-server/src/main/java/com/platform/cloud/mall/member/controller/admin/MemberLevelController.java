package com.platform.cloud.mall.member.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.member.dto.LevelRequest;
import com.platform.cloud.mall.member.service.MemberLevelService;
import com.platform.cloud.mall.member.vo.LevelVO;
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
 * 会员等级。
 */
@Tag(name = "会员等级")
@RestController
@RequestMapping("/admin/member/level")
@RequiredArgsConstructor
public class MemberLevelController {

    private final MemberLevelService levelService;

    @Operation(summary = "等级分页")
    @PreAuthorize("@ss.has('member:level:view')")
    @GetMapping("/page")
    public R<PageVO<LevelVO>> page(@Valid PageQuery query) {
        return R.ok(levelService.page(query));
    }

    /** 会员列表按等级筛选要用，所以有会员查看权限也可以调用 */
    @Operation(summary = "全部等级")
    @PreAuthorize("@ss.has('member:level:view') or @ss.has('member:member:view')")
    @GetMapping("/list")
    public R<List<LevelVO>> list() {
        return R.ok(levelService.list());
    }

    @Operation(summary = "等级详情")
    @PreAuthorize("@ss.has('member:level:view')")
    @GetMapping("/{id}")
    public R<LevelVO> detail(@PathVariable Long id) {
        return R.ok(levelService.detail(id));
    }

    @Operation(summary = "新增等级")
    @PreAuthorize("@ss.has('member:level:edit')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody LevelRequest req) {
        return R.ok(levelService.create(req));
    }

    @Operation(summary = "修改等级")
    @PreAuthorize("@ss.has('member:level:edit')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody LevelRequest req) {
        levelService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除等级")
    @PreAuthorize("@ss.has('member:level:edit')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        levelService.delete(id);
        return R.ok();
    }
}

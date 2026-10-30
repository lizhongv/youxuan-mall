package com.platform.cloud.mall.member.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.member.dto.AccountLogQuery;
import com.platform.cloud.mall.member.dto.AdjustRequest;
import com.platform.cloud.mall.member.dto.MemberQuery;
import com.platform.cloud.mall.member.service.MemberAccountService;
import com.platform.cloud.mall.member.service.MemberAdminService;
import com.platform.cloud.mall.member.vo.AccountLogVO;
import com.platform.cloud.mall.member.vo.MemberAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台会员管理与账户流水。
 */
@Tag(name = "会员管理")
@RestController
@RequestMapping("/admin/member")
@RequiredArgsConstructor
public class MemberAdminController {

    private final MemberAdminService memberAdminService;
    private final MemberAccountService accountService;

    @Operation(summary = "会员分页")
    @PreAuthorize("@ss.has('member:member:view')")
    @GetMapping("/member/page")
    public R<PageVO<MemberAdminVO>> page(@Valid MemberQuery query) {
        return R.ok(memberAdminService.page(query));
    }

    @Operation(summary = "会员详情")
    @PreAuthorize("@ss.has('member:member:view')")
    @GetMapping("/member/{id}")
    public R<MemberAdminVO> detail(@PathVariable Long id) {
        return R.ok(memberAdminService.detail(id));
    }

    @Operation(summary = "冻结 / 解冻会员")
    @PreAuthorize("@ss.has('member:member:edit')")
    @PutMapping("/member/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberAdminService.updateStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "调整余额 / 积分")
    @PreAuthorize("@ss.has('member:member:edit')")
    @PostMapping("/member/{id}/adjust")
    public R<Void> adjust(@PathVariable Long id, @Valid @RequestBody AdjustRequest req) {
        memberAdminService.adjust(id, req);
        return R.ok();
    }

    @Operation(summary = "账户流水分页")
    @PreAuthorize("@ss.has('member:member:view')")
    @GetMapping("/account-log/page")
    public R<PageVO<AccountLogVO>> accountLogs(@Valid AccountLogQuery query) {
        return R.ok(accountService.pageLogs(query));
    }
}

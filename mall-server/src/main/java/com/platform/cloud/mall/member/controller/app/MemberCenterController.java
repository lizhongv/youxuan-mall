package com.platform.cloud.mall.member.controller.app;

import com.platform.cloud.bootstrap.common.security.JwtProperties;
import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.member.dto.AccountLogQuery;
import com.platform.cloud.mall.member.dto.MemberLogoutRequest;
import com.platform.cloud.mall.member.dto.MemberPasswordRequest;
import com.platform.cloud.mall.member.dto.MemberProfileRequest;
import com.platform.cloud.mall.member.service.MemberAccountService;
import com.platform.cloud.mall.member.service.MemberAuthService;
import com.platform.cloud.mall.member.service.MemberProfileService;
import com.platform.cloud.mall.member.vo.AccountLogVO;
import com.platform.cloud.mall.member.vo.MemberProfileVO;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.system.service.FileService;
import com.platform.cloud.mall.system.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 会员中心：资料、密码、账户流水、上传、登出。
 */
@Tag(name = "会员中心")
@RestController
@RequestMapping("/app/member")
@RequiredArgsConstructor
public class MemberCenterController {

    /** 商城端只允许这几类上传，其余归为 member，避免会员上传内容混进商品图等后台分类 */
    private static final Set<String> APP_BIZ_TYPES = Set.of("avatar", "review", "aftersale");

    private final MemberProfileService profileService;
    private final MemberAccountService accountService;
    private final MemberAuthService authService;
    private final FileService fileService;
    private final JwtProperties jwtProperties;

    @Operation(summary = "我的资料")
    @GetMapping("/profile")
    public R<MemberProfileVO> profile() {
        return R.ok(profileService.profile(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "修改资料")
    @PutMapping("/profile")
    public R<Void> updateProfile(@Valid @RequestBody MemberProfileRequest req) {
        profileService.updateProfile(SecurityUtils.currentUserId(), req);
        return R.ok();
    }

    @Operation(summary = "修改密码（返回新令牌，旧令牌全部失效）")
    @PutMapping("/password")
    public R<MemberTokenVO> changePassword(@Valid @RequestBody MemberPasswordRequest req) {
        return R.ok(profileService.changePassword(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "我的账户流水")
    @GetMapping("/account-log/page")
    public R<PageVO<AccountLogVO>> accountLogs(@Valid AccountLogQuery query) {
        // 忽略前端传的 memberId，只能查自己的
        query.setMemberId(SecurityUtils.currentUserId());
        return R.ok(accountService.pageLogs(query));
    }

    @Operation(summary = "上传图片（头像、评价图）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<FileVO> upload(@RequestPart("file") MultipartFile file,
                            @RequestParam(value = "bizType", required = false) String bizType) {
        String type = bizType != null && APP_BIZ_TYPES.contains(bizType) ? bizType : "member";
        return R.ok(fileService.uploadByMember(SecurityUtils.currentUserId(), file, type));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request,
                          @Valid @RequestBody(required = false) MemberLogoutRequest req) {
        String header = request.getHeader(jwtProperties.getHeader());
        String prefix = jwtProperties.getTokenPrefix();
        String access = header != null && header.startsWith(prefix) ? header.substring(prefix.length()).trim() : null;
        authService.logout(access, req == null ? null : req.getRefreshToken());
        return R.ok();
    }
}

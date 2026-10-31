package com.platform.cloud.mall.member.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.member.dto.WechatBindRequest;
import com.platform.cloud.mall.member.dto.WechatLoginRequest;
import com.platform.cloud.mall.member.service.MemberWechatAuthService;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.member.vo.WechatAuthorizeVO;
import com.platform.cloud.mall.member.vo.WechatLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信公众号网页授权登录，公开访问（/app/auth/**）。
 */
@Tag(name = "会员认证-微信")
@Validated
@RestController
@RequestMapping("/app/auth/wechat")
@RequiredArgsConstructor
public class MemberWechatAuthController {

    private final MemberWechatAuthService wechatAuthService;

    @Operation(summary = "获取公众号网页授权地址（snsapi_base）")
    @GetMapping("/authorize-url")
    public R<WechatAuthorizeVO> authorizeUrl(@RequestParam @NotBlank String redirect) {
        return R.ok(wechatAuthService.authorizeUrl(redirect));
    }

    @Operation(summary = "授权回跳后用 code 登录；未绑定返回 needBind 与 bindToken")
    @PostMapping("/login")
    public R<WechatLoginVO> login(@Valid @RequestBody WechatLoginRequest req) {
        return R.ok(wechatAuthService.login(req.getCode(), req.getState()));
    }

    @Operation(summary = "绑定手机号（验证码场景 LOGIN，未注册自动注册）并登录")
    @PostMapping("/bind")
    public R<MemberTokenVO> bind(@Valid @RequestBody WechatBindRequest req) {
        return R.ok(wechatAuthService.bind(req));
    }
}

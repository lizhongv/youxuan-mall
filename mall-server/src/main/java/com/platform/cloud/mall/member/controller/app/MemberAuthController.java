package com.platform.cloud.mall.member.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.member.dto.MemberLoginRequest;
import com.platform.cloud.mall.member.dto.MemberRefreshRequest;
import com.platform.cloud.mall.member.dto.MemberRegisterRequest;
import com.platform.cloud.mall.member.dto.MemberResetPasswordRequest;
import com.platform.cloud.mall.member.dto.SmsCodeRequest;
import com.platform.cloud.mall.member.dto.SmsLoginRequest;
import com.platform.cloud.mall.member.service.MemberAuthService;
import com.platform.cloud.mall.member.service.SmsCodeService;
import com.platform.cloud.mall.member.support.SmsScene;
import com.platform.cloud.mall.member.vo.MemberTokenVO;
import com.platform.cloud.mall.member.vo.SmsCodeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商城端认证，全部公开访问。
 */
@Tag(name = "会员认证")
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class MemberAuthController {

    private final SmsCodeService smsCodeService;
    private final MemberAuthService authService;

    @Operation(summary = "发送短信验证码")
    @PostMapping("/sms-code")
    public R<SmsCodeVO> smsCode(@Valid @RequestBody SmsCodeRequest req) {
        return R.ok(smsCodeService.send(req.getMobile(), SmsScene.valueOf(req.getScene())));
    }

    @Operation(summary = "手机号注册")
    @PostMapping("/register")
    public R<MemberTokenVO> register(@Valid @RequestBody MemberRegisterRequest req) {
        return R.ok(authService.register(req));
    }

    @Operation(summary = "密码登录")
    @PostMapping("/login")
    public R<MemberTokenVO> login(@Valid @RequestBody MemberLoginRequest req) {
        return R.ok(authService.login(req));
    }

    @Operation(summary = "验证码登录（未注册自动注册）")
    @PostMapping("/login-sms")
    public R<MemberTokenVO> loginBySms(@Valid @RequestBody SmsLoginRequest req) {
        return R.ok(authService.loginBySms(req));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public R<MemberTokenVO> refresh(@Valid @RequestBody MemberRefreshRequest req) {
        return R.ok(authService.refresh(req.getRefreshToken()));
    }

    @Operation(summary = "验证码重置密码")
    @PostMapping("/reset-password")
    public R<Void> resetPassword(@Valid @RequestBody MemberResetPasswordRequest req) {
        authService.resetPassword(req);
        return R.ok();
    }
}

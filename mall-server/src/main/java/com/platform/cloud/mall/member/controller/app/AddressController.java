package com.platform.cloud.mall.member.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.common.security.SecurityUtils;
import com.platform.cloud.mall.member.dto.AddressRequest;
import com.platform.cloud.mall.member.service.AddressService;
import com.platform.cloud.mall.member.vo.AddressVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 收货地址。
 */
@Tag(name = "收货地址")
@RestController
@RequestMapping("/app/member/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @Operation(summary = "地址列表")
    @GetMapping
    public R<List<AddressVO>> list() {
        return R.ok(addressService.list(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "默认地址")
    @GetMapping("/default")
    public R<AddressVO> getDefault() {
        return R.ok(addressService.getDefault(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "地址详情")
    @GetMapping("/{id}")
    public R<AddressVO> detail(@PathVariable Long id) {
        return R.ok(addressService.detail(SecurityUtils.currentUserId(), id));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public R<Long> create(@Valid @RequestBody AddressRequest req) {
        return R.ok(addressService.create(SecurityUtils.currentUserId(), req));
    }

    @Operation(summary = "修改地址")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody AddressRequest req) {
        addressService.update(SecurityUtils.currentUserId(), id, req);
        return R.ok();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        addressService.delete(SecurityUtils.currentUserId(), id);
        return R.ok();
    }

    @Operation(summary = "设为默认地址")
    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(SecurityUtils.currentUserId(), id);
        return R.ok();
    }
}

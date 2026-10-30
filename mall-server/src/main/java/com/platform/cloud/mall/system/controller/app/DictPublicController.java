package com.platform.cloud.mall.system.controller.app;

import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.service.SysDictService;
import com.platform.cloud.mall.system.vo.DictItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商城端字典：取消原因、售后原因等下拉选项，与后台维护的字典保持一致而不是写死在前端。
 */
@Tag(name = "公开字典")
@RestController
@RequestMapping("/app/public/dict")
@RequiredArgsConstructor
public class DictPublicController {

    private final SysDictService dictService;

    @Operation(summary = "按类型取启用的字典项")
    @GetMapping("/{dictType}")
    public R<List<DictItemVO>> items(@PathVariable String dictType) {
        return R.ok(dictService.items(dictType));
    }
}

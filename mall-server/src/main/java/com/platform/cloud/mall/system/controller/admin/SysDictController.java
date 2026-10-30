package com.platform.cloud.mall.system.controller.admin;

import com.platform.cloud.bootstrap.common.web.PageVO;
import com.platform.cloud.bootstrap.common.web.R;
import com.platform.cloud.mall.system.dto.DictQuery;
import com.platform.cloud.mall.system.dto.DictSaveRequest;
import com.platform.cloud.mall.system.service.SysDictService;
import com.platform.cloud.mall.system.vo.DictItemVO;
import com.platform.cloud.mall.system.vo.DictVO;
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
 * 数据字典。
 */
@Tag(name = "数据字典")
@RestController
@RequestMapping("/admin/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @Operation(summary = "字典分页")
    @PreAuthorize("@ss.has('system:dict:view')")
    @GetMapping("/page")
    public R<PageVO<DictVO>> page(@Valid DictQuery query) {
        return R.ok(dictService.page(query));
    }

    @Operation(summary = "全部字典类型")
    @PreAuthorize("@ss.has('system:dict:view')")
    @GetMapping("/types")
    public R<List<String>> types() {
        return R.ok(dictService.types());
    }

    /**
     * 下拉选项（如发货时选快递公司）。只要求登录：运营没有字典管理权限也要能用。
     */
    @Operation(summary = "按类型取启用的字典项")
    @GetMapping("/data/{dictType}")
    public R<List<DictItemVO>> data(@PathVariable String dictType) {
        return R.ok(dictService.items(dictType));
    }

    @Operation(summary = "字典详情")
    @PreAuthorize("@ss.has('system:dict:view')")
    @GetMapping("/{id}")
    public R<DictVO> detail(@PathVariable Long id) {
        return R.ok(dictService.detail(id));
    }

    @Operation(summary = "新增字典项")
    @PreAuthorize("@ss.has('system:dict:edit')")
    @PostMapping
    public R<Long> create(@Valid @RequestBody DictSaveRequest req) {
        return R.ok(dictService.create(req));
    }

    @Operation(summary = "修改字典项")
    @PreAuthorize("@ss.has('system:dict:edit')")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody DictSaveRequest req) {
        dictService.update(id, req);
        return R.ok();
    }

    @Operation(summary = "删除字典项")
    @PreAuthorize("@ss.has('system:dict:edit')")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        dictService.delete(id);
        return R.ok();
    }
}

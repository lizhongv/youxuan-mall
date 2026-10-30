package com.platform.cloud.mall.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 新增 / 修改字典项 */
@Data
public class DictSaveRequest {

    @NotBlank
    @Pattern(regexp = "^[a-z][a-z0-9_]{1,63}$", message = "字典类型为小写字母开头的小写字母、数字或下划线")
    private String dictType;

    @NotBlank
    @Size(max = 64)
    private String label;

    @NotBlank
    @Size(max = 64)
    private String value;

    private Integer sort;

    @Min(0)
    @Max(1)
    private Integer status;

    @Size(max = 255)
    private String remark;
}

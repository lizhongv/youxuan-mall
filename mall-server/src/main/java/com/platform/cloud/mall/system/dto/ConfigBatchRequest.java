package com.platform.cloud.mall.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 批量修改系统参数：只能改已存在的键，新增键需随迁移脚本发布，避免后台随手造出代码不认识的键 */
@Data
public class ConfigBatchRequest {

    @NotEmpty
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @NotBlank
        private String configKey;

        /** 4096：容纳 RSA 4096 位 PEM 私钥；敏感参数提交掩码 ****** 表示保持原值 */
        @NotNull
        @Size(max = 4096)
        private String configValue;
    }
}

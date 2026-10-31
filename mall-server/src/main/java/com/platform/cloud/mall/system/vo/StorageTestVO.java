package com.platform.cloud.mall.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 对象存储测试结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageTestVO {

    @Schema(description = "当前存储方式 LOCAL / S3")
    private String provider;

    @Schema(description = "探测文件的访问地址（已删除，仅用于核对域名拼接是否正确）")
    private String sampleUrl;

    @Schema(description = "上传 + 删除耗时（毫秒）")
    private long elapsedMs;

    @Schema(description = "说明")
    private String message;
}

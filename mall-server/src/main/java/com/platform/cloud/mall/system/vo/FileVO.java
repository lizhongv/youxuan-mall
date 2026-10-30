package com.platform.cloud.mall.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 上传结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileVO {

    private Long id;
    private String url;
    private String originalName;
    /** 字节数；用 Integer 输出为数字（上传上限远小于 2GB） */
    private Integer size;
}

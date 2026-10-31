package com.platform.cloud.mall.system.vo;

import lombok.Data;

/** 系统参数 */
@Data
public class ConfigVO {

    private Long id;
    private String configKey;
    private String configValue;
    private String name;
    private String remark;
    /** 敏感参数：configValue 已设置时返回掩码 ******，未设置时为空串 */
    private Boolean sensitive;
}

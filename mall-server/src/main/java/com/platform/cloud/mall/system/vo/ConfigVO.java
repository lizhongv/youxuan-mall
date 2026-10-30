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
}

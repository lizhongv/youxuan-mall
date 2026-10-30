package com.platform.cloud.mall.system.vo;

import lombok.Data;

/** 字典项 */
@Data
public class DictVO {

    private Long id;
    private String dictType;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
    private String remark;
}

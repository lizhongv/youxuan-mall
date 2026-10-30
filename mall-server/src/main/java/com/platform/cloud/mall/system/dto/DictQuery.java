package com.platform.cloud.mall.system.dto;

import com.platform.cloud.bootstrap.common.web.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 字典分页：keyword 匹配标签 / 值 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DictQuery extends PageQuery {

    private String dictType;
    private Integer status;
}

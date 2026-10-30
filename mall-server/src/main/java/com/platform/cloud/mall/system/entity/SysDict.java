package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 数据字典项 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict")
public class SysDict extends BaseEntity {

    private String dictType;
    private String label;
    private String value;
    private Integer sort;
    private Integer status;
    private String remark;
}

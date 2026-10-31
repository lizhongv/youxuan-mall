package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 运营参数 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private String configKey;
    private String configValue;
    private String name;
    private String remark;
    /**
     * 1 敏感值（私钥、密钥）：后台列表只返回掩码，提交掩码表示不修改。
     * SENSITIVE 是 MySQL 8 保留字，列名必须加反引号。
     */
    @TableField("`sensitive`")
    private Integer sensitive;
}

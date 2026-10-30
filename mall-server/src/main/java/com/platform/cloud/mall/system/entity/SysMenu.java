package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.cloud.bootstrap.common.mybatis.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 菜单与按钮权限，一棵树同时用于前端菜单展示和角色授权 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    public static final String TYPE_DIR = "DIR";
    public static final String TYPE_MENU = "MENU";
    public static final String TYPE_BUTTON = "BUTTON";

    private Long parentId;
    private String type;
    private String name;
    private String path;
    private String component;
    private String icon;
    private String permission;
    private Integer sort;
    private Integer visible;
    private Integer status;
}

package com.platform.cloud.mall.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。菜单管理返回全部字段；/admin/auth/me 只需要 MenuTree 部分，permission / status 置空后不输出。
 */
@Data
public class MenuTreeVO {

    private Long id;
    private Long parentId;
    private String type;
    private String name;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer visible;
    private String permission;
    private Integer status;
    private List<MenuTreeVO> children = new ArrayList<>();
}

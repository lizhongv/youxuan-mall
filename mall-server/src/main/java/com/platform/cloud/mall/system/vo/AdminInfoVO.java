package com.platform.cloud.mall.system.vo;

import lombok.Data;

import java.util.List;

/** 当前登录管理员信息，前端据此渲染菜单与按钮 */
@Data
public class AdminInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private List<String> roles;
    private List<String> permissions;
    private List<MenuTreeVO> menus;
    /** true 时前端应直接跳到修改密码页：其他管理端接口会返回 40301 */
    private Boolean mustChangePassword;
}

package com.platform.cloud.mall.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 管理员-角色关联。纯关联表没有审计列，因此不继承 BaseEntity，删除为物理删除 */
@Data
@NoArgsConstructor
@TableName("sys_admin_role")
public class SysAdminRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long adminId;
    private Long roleId;

    public SysAdminRole(Long adminId, Long roleId) {
        this.adminId = adminId;
        this.roleId = roleId;
    }
}

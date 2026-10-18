package com.yuxi.ich.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;

/**
 * 系统角色。
 *
 * <p>表：sys_role。role_code 与 {@code com.yuxi.ich.common.constant.RoleConstants} 对应。
 */
@TableName("sys_role")
public class SysRole extends BaseEntity implements Serializable {

    /** 角色名称，如 平台管理员 */
    private String roleName;
    /** 角色编码，如 ROLE_ADMIN */
    private String roleCode;
    /** 说明 */
    private String remark;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

package com.yuxi.ich.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

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

    /** 角色表不维护更新时间，排除父类继承的 update_time 列映射（MySQL sys_role 无此列） */
    @TableField(exist = false)
    private LocalDateTime updateTime;

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

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

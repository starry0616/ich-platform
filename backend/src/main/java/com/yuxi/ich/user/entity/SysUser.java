package com.yuxi.ich.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户。
 *
 * <p>表：sys_user。三类角色共用一张表，通过 role_id 区分。
 */
@TableName("sys_user")
public class SysUser extends BaseEntity implements Serializable {

    /** 登录用户名，唯一 */
    private String username;
    /** BCrypt 加盐哈希后的密码，绝不返回给前端 */
    @JsonIgnore
    private String password;
    /** 昵称 */
    private String nickname;
    /** 头像地址 */
    private String avatar;
    /** 手机号 */
    private String phone;
    /** 电子邮箱 */
    private String email;
    /** 真实姓名 */
    private String realName;
    /** 证件号码（加密存储） */
    private String idCard;
    /** 角色 id，关联 sys_role */
    private Long roleId;
    /** 账号状态：0 禁用，1 正常 */
    private Integer status;
    /** 最近登录时间 */
    private LocalDateTime lastLoginTime;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }
}

package com.yuxi.ich.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 登录用户上下文。
 *
 * <p>认证成功后由过滤器放入 SecurityContext，业务代码可通过
 * {@link SecurityUtils#getUserId()} 等方法直接取用，无需层层传参。
 */
public class LoginUser implements Serializable {

    private Long userId;
    private String username;
    private String nickname;
    private Long roleId;
    private String roleCode;
    private Integer status;

    public LoginUser() {
    }

    public LoginUser(Long userId, String username, String nickname,
                     Long roleId, String roleCode, Integer status) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.roleId = roleId;
        this.roleCode = roleCode;
        this.status = status;
    }

    /** 是否管理员 */
    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(roleCode);
    }

    /** 是否传承人 */
    public boolean isInheritor() {
        return "ROLE_INHERITOR".equals(roleCode);
    }

    /** Spring Security 使用的权限集合 */
    public Collection<String> getAuthorities() {
        if (roleCode == null || roleCode.isBlank()) {
            return Collections.emptyList();
        }
        return List.of(roleCode);
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}

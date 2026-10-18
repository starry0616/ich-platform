package com.yuxi.ich.auth.dto;

/**
 * 登录成功返回。
 *
 * <p>双令牌：accessToken 用于业务请求，refreshToken 仅用于换取新的 accessToken。
 * 前端把 accessToken 存 Pinia 并附加到请求头；过期后用 refreshToken 换新。
 */
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    /** 令牌类型，固定 Bearer */
    private String tokenType = "Bearer";
    /** accessToken 有效期（秒） */
    private Long expiresIn;
    private Long userId;
    private String username;
    private String nickname;
    private String roleCode;
    private String avatar;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, Long expiresIn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
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

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}

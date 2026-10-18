package com.yuxi.ich.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 *
 * <p>密码强度要求：长度 8-64 位且同时包含字母与数字。
 * 该规则同时在前端做一次，前端负责体验，后端负责安全。
 */
public class RegisterRequest {

    @NotBlank(message = "请输入用户名")
    @Size(min = 3, max = 50, message = "用户名长度应为 3-50 位")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字与下划线")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 8, max = 64, message = "密码长度应为 8-64 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码需同时包含字母和数字")
    private String password;

    @Size(max = 50, message = "昵称不能超过 50 字")
    private String nickname;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^$|^[\\w.-]+@[\\w-]+\\.[\\w.]+$", message = "邮箱格式不正确")
    private String email;

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
}

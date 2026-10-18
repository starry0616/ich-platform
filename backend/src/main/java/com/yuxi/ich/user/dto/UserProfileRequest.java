package com.yuxi.ich.user.dto;

import jakarta.validation.constraints.Size;

/**
 * 个人资料修改请求（对应接口 6）。
 */
public class UserProfileRequest {

    @Size(max = 50, message = "昵称不能超过 50 字")
    private String nickname;

    @Size(max = 20, message = "手机号格式不正确")
    private String phone;

    @Size(max = 100, message = "邮箱不能超过 100 字")
    private String email;

    @Size(max = 50, message = "真实姓名不能超过 50 字")
    private String realName;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}

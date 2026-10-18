package com.yuxi.ich.activity.dto;

import jakarta.validation.constraints.Size;

/**
 * 活动报名请求体。
 *
 * <p>用 JSON 请求体而非查询参数：报名信息属于提交数据，
 * 且姓名、电话、备注可能含中文与特殊字符，放在 body 里更规范、也便于后续扩展字段。
 */
public class SignupRequest {

    /** 报名姓名，留空则取当前账号昵称 */
    @Size(max = 50, message = "姓名不能超过 50 字")
    private String realName;

    /** 联系电话 */
    @Size(max = 20, message = "电话不能超过 20 字")
    private String phone;

    /** 备注 / 特殊需求 */
    @Size(max = 255, message = "备注不能超过 255 字")
    private String remark;

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

package com.yuxi.ich.activity.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动报名记录。
 *
 * <p>表：act_signup。
 * <p>表上有唯一约束 (activity_id, user_id)，从数据层杜绝同一用户重复报名，
 * 这是防超卖之外的另一重保障。
 */
@TableName("act_signup")
public class ActSignup extends BaseEntity implements Serializable {

    /** 活动 id */
    private Long activityId;
    /** 报名用户 id */
    private Long userId;
    /** 报名编号，业务唯一，供现场核销 */
    private String signupNo;
    /** 报名时填写的姓名（快照，避免用户后续改名影响核销） */
    private String realName;
    /** 联系电话（快照） */
    private String phone;
    /** 备注 / 特殊需求 */
    private String remark;
    /** 报名时间 */
    private LocalDateTime signupTime;
    /** 取消时间 */
    private LocalDateTime cancelTime;
    /** 状态：1 已报名，2 已取消，3 已核销，4 已释放 */
    private Integer status;
    /** 核销状态：0 未核销，1 已核销 */
    private Integer checkinStatus;
    /** 核销时间 */
    private LocalDateTime checkinTime;
    /** 核销操作人 */
    private Long checkinBy;

    /** 报名表不维护更新时间，排除父类继承的 update_time 列映射（MySQL act_signup 无此列） */
    @TableField(exist = false)
    private LocalDateTime updateTime;

    /** 活动名称：非数据库字段，我的报名列表展示用 */
    @TableField(exist = false)
    private String activityTitle;
    /** 活动开始时间：非数据库字段 */
    @TableField(exist = false)
    private LocalDateTime activityStartTime;
    /** 活动地点：非数据库字段 */
    @TableField(exist = false)
    private String activityAddress;

    // ---------------- getter / setter ----------------

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSignupNo() {
        return signupNo;
    }

    public void setSignupNo(String signupNo) {
        this.signupNo = signupNo;
    }

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

    public LocalDateTime getSignupTime() {
        return signupTime;
    }

    public void setSignupTime(LocalDateTime signupTime) {
        this.signupTime = signupTime;
    }

    public LocalDateTime getCancelTime() {
        return cancelTime;
    }

    public void setCancelTime(LocalDateTime cancelTime) {
        this.cancelTime = cancelTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getCheckinStatus() {
        return checkinStatus;
    }

    public void setCheckinStatus(Integer checkinStatus) {
        this.checkinStatus = checkinStatus;
    }

    public LocalDateTime getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(LocalDateTime checkinTime) {
        this.checkinTime = checkinTime;
    }

    public Long getCheckinBy() {
        return checkinBy;
    }

    public void setCheckinBy(Long checkinBy) {
        this.checkinBy = checkinBy;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle) {
        this.activityTitle = activityTitle;
    }

    public LocalDateTime getActivityStartTime() {
        return activityStartTime;
    }

    public void setActivityStartTime(LocalDateTime activityStartTime) {
        this.activityStartTime = activityStartTime;
    }

    public String getActivityAddress() {
        return activityAddress;
    }

    public void setActivityAddress(String activityAddress) {
        this.activityAddress = activityAddress;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}

package com.yuxi.ich.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 内容审核记录表（sys_audit_log，说明书 7.3 节）。
 *
 * <p>记录审核人、审核时间、审核结论与意见，便于责任追溯；
 * 同时兼作管理员「操作日志」数据源（接口 70 /api/admin/log）。
 */
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 1 项目，2 资讯，3 活动，4 传承人，5 评论，6 用户 */
    private Integer targetType;

    private Long targetId;

    /** 审核人（管理员）id */
    private Long auditorId;

    /** 1 通过，2 驳回，3 下架，4 禁用，5 其他 */
    private Integer action;

    private String remark;

    private LocalDateTime createTime;

    /** 操作人姓名（冗余展示用，非表字段，由查询端联表补充） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String auditorName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(Long auditorId) {
        this.auditorId = auditorId;
    }

    public Integer getAction() {
        return action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getAuditorName() {
        return auditorName;
    }

    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }
}

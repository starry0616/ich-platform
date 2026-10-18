package com.yuxi.ich.interaction.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论举报表（interaction_report）。
 *
 * <p>支撑评论举报（说明书表 6-4 接口 41）与管理员举报处理
 * （接口 65 评论治理）：举报记录进入待处理队列，管理员处理后回填结论。
 */
@TableName("interaction_report")
public class InteractionReport implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 被举报的互动记录（评论）id */
    private Long interactionId;

    /** 举报人 */
    private Long userId;

    private String reason;

    /** 0 待处理，1 已处理，2 已驳回 */
    private Integer status;

    /** 处理人 */
    private Long handleBy;

    private LocalDateTime handleTime;

    /** 处理意见 */
    private String remark;

    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(Long interactionId) {
        this.interactionId = interactionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getHandleBy() {
        return handleBy;
    }

    public void setHandleBy(Long handleBy) {
        this.handleBy = handleBy;
    }

    public LocalDateTime getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(LocalDateTime handleTime) {
        this.handleTime = handleTime;
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
}

package com.yuxi.ich.message.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

/**
 * 消息通知表（sys_message）。
 *
 * <p>覆盖报名成功、审核结果、互动回复等站内通知（说明书表 6-4 接口 42）。
 */
@TableName("sys_message")
public class SysMessage extends BaseEntity {

    /** 接收人 */
    private Long userId;

    private String title;

    private String content;

    /** 1 系统，2 报名，3 审核，4 互动 */
    private Integer msgType;

    /** 关联业务主键（活动 id / 评论 id 等） */
    private Long bizId;

    /** 0 未读，1 已读 */
    private Integer isRead;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public Integer getIsRead() {
        return isRead;
    }

    public void setIsRead(Integer isRead) {
        this.isRead = isRead;
    }
}

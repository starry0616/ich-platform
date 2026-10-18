package com.yuxi.ich.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户浏览历史表（user_browse_history）。
 *
 * <p>支撑「我的浏览历史」接口（说明书表 6-2 接口 10）：
 * 同一用户对同一目标只保留一条记录，重复浏览更新 last_view_time 并累计次数。
 */
@TableName("user_browse_history")
public class UserBrowseHistory implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 1 非遗项目，2 文化资讯，3 非遗活动 */
    private Integer targetType;

    private Long targetId;

    /** 最近浏览时间 */
    private LocalDateTime lastViewTime;

    /** 累计浏览次数 */
    private Integer viewCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public LocalDateTime getLastViewTime() {
        return lastViewTime;
    }

    public void setLastViewTime(LocalDateTime lastViewTime) {
        this.lastViewTime = lastViewTime;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
}

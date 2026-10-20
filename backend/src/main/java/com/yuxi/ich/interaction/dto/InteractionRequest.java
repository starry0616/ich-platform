package com.yuxi.ich.interaction.dto;

import com.yuxi.ich.common.query.BaseQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 互动操作请求体（收藏 / 点赞 / 评论）。
 */
public class InteractionRequest {

    /** 目标类型：1 非遗项目，2 文化资讯，3 非遗活动，4 传承人 */
    @NotNull(message = "请指定互动目标类型")
    @Min(value = 1, message = "目标类型不合法")
    @Max(value = 4, message = "目标类型不合法")
    private Integer targetType;

    /** 目标对象 id */
    @NotNull(message = "请指定互动目标")
    private Long targetId;

    /**
     * 互动类型：1 收藏，2 点赞，3 评论。
     * 也可由 URL 路径指定，两者取其一。
     */
    @Min(value = 1, message = "互动类型不合法")
    @Max(value = 3, message = "互动类型不合法")
    private Integer type;

    /** 评论内容，type=3 时必填 */
    private String content;

    /** 父评论 id，回复评论时传入，顶层评论传 0 或留空 */
    private Long parentId;

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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    /**
     * 评论查询参数。
     */
    public static class CommentQuery extends BaseQuery {
        /** 目标类型 */
        private Integer targetType;
        /** 目标对象 id */
        private Long targetId;

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
    }
}

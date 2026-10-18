package com.yuxi.ich.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.io.Serializable;

/**
 * 互动记录。
 *
 * <p>表：interaction。用<b>多态关联</b>一张表承载三类对象（项目/资讯/活动）
 * 的三种互动（收藏/点赞/评论），避免为每种组合单独建表。
 *
 * <p>代价与补偿：
 * <ul>
 *   <li>代价：无法用数据库外键保证引用完整性</li>
 *   <li>补偿：在应用层做级联清理（删除项目时同步清理其互动记录）；
 *       并用唯一约束 (user_id, target_type, target_id, type) 保证幂等</li>
 * </ul>
 */
@TableName("interaction")
public class Interaction extends BaseEntity implements Serializable {

    /** 发起互动的用户 id */
    private Long userId;
    /** 目标类型：1 非遗项目，2 文化资讯，3 非遗活动 */
    private Integer targetType;
    /** 目标对象主键 */
    private Long targetId;
    /** 互动类型：1 收藏，2 点赞，3 评论 */
    private Integer type;
    /** 评论内容（非评论类型为空） */
    private String content;
    /** 父评论 id，0 为顶层评论 */
    private Long parentId;
    /** 评论获赞数 */
    private Integer likeCount;
    /** 状态：0 已屏蔽，1 正常 */
    private Integer status;

    /** 评论人昵称：非数据库字段，列表展示用 */
    @TableField(exist = false)
    private String userNickname;
    /** 评论人头像：非数据库字段 */
    @TableField(exist = false)
    private String userAvatar;
    /** 目标对象标题：非数据库字段，我的收藏列表展示用 */
    @TableField(exist = false)
    private String targetTitle;
    /** 目标对象封面：非数据库字段 */
    @TableField(exist = false)
    private String targetCover;

    // ---------------- getter / setter ----------------

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

    public Integer getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(Integer likeCount) {
        this.likeCount = likeCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public void setTargetTitle(String targetTitle) {
        this.targetTitle = targetTitle;
    }

    public String getTargetCover() {
        return targetCover;
    }

    public void setTargetCover(String targetCover) {
        this.targetCover = targetCover;
    }
}

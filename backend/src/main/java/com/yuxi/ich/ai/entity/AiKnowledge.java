package com.yuxi.ich.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

/**
 * AI 知识库文档表（ai_knowledge，说明书 5.4 / 4.7 节）。
 *
 * <p>存储已审核通过的非遗资料原文；只有审核通过的内容参与检索。
 */
@TableName("ai_knowledge")
public class AiKnowledge extends BaseEntity {

    private String title;

    /** 1 非遗项目，2 传承人，3 资讯，4 政策/术语 */
    private Integer sourceType;

    /** 来源业务对象主键（可空） */
    private Long sourceId;

    /** 类别元数据（检索过滤） */
    private Long categoryId;

    /** 地域元数据（检索过滤） */
    private String region;

    /** 文档全文 */
    private String content;

    /** 切片数量 */
    private Integer chunkCount;

    /** 0 待索引，1 已索引，2 索引失败 */
    private Integer indexStatus;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getSourceType() {
        return sourceType;
    }

    public void setSourceType(Integer sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public Integer getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(Integer indexStatus) {
        this.indexStatus = indexStatus;
    }
}

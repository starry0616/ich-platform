package com.yuxi.ich.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 知识切片表（ai_knowledge_chunk）。
 *
 * <p>与 ai_knowledge 构成一对多：按语义段落切分后的切片，
 * 单块长度约 500 字并保留重叠；vector_id 预留向量库标识。
 */
@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long knowledgeId;

    private Integer chunkIndex;

    private String content;

    /** 向量库中的标识（接入向量库后回填） */
    private String vectorId;

    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVectorId() {
        return vectorId;
    }

    public void setVectorId(String vectorId) {
        this.vectorId = vectorId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

package com.yuxi.ich.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.yuxi.ich.common.entity.BaseEntity;

import java.math.BigDecimal;

/**
 * AI 问答记录表（ai_chat，说明书 5.4 / 4.7 节）。
 *
 * <p>记录提问原文、回答、引用来源、检索得分等，支撑问答历史、
 * 用户反馈与管理员复核；top_score 低于阈值的问题计入「无结果问题清单」。
 */
@TableName("ai_chat")
public class AiChat extends BaseEntity {

    /** 提问用户（游客可为空） */
    private Long userId;

    /** 会话标识（多轮对话归组） */
    private String sessionId;

    private String question;

    private String answer;

    /** 引用来源列表（JSON 数组） */
    private String refs;

    /** 最高检索相似度 */
    private BigDecimal topScore;

    /** 命中知识切片数量 */
    private Integer hitCount;

    /** 用户反馈：1 有帮助，0 无帮助，null 未评价 */
    private Integer isUseful;

    /** 0 未复核，1 已复核通过，2 已标记问题 */
    private Integer reviewStatus;

    private Integer tokenUsed;

    /** 处理耗时（毫秒） */
    private Integer costMs;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getRefs() {
        return refs;
    }

    public void setRefs(String refs) {
        this.refs = refs;
    }

    public BigDecimal getTopScore() {
        return topScore;
    }

    public void setTopScore(BigDecimal topScore) {
        this.topScore = topScore;
    }

    public Integer getHitCount() {
        return hitCount;
    }

    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }

    public Integer getIsUseful() {
        return isUseful;
    }

    public void setIsUseful(Integer isUseful) {
        this.isUseful = isUseful;
    }

    public Integer getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(Integer reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public Integer getTokenUsed() {
        return tokenUsed;
    }

    public void setTokenUsed(Integer tokenUsed) {
        this.tokenUsed = tokenUsed;
    }

    public Integer getCostMs() {
        return costMs;
    }

    public void setCostMs(Integer costMs) {
        this.costMs = costMs;
    }
}

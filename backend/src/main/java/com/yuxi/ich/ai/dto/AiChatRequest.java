package com.yuxi.ich.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 提问请求（接口 43）。
 */
public class AiChatRequest {

    @NotBlank(message = "请输入问题")
    @Size(max = 1000, message = "问题不能超过 1000 字")
    private String question;

    /** 会话标识（多轮归组，前端生成后回传；缺省时后端创建） */
    private String sessionId;

    /** 可选：限定类别检索 */
    private Long categoryId;

    /** 可选：限定地域检索 */
    private String region;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
}

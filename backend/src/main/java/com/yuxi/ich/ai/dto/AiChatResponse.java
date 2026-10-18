package com.yuxi.ich.ai.dto;

import java.util.List;

/**
 * AI 问答响应（接口 43）。
 *
 * <p>设计文档 4.7.2 描述 SSE 流式输出；本实现为保持接口简单与
 * 前后端联调稳定，先返回完整 JSON（含 answer 与引用来源），
 * 前端按消息气泡渲染，后续可平滑升级为 SSE。
 */
public class AiChatResponse {

    /** 问答记录 id（用于反馈与来源查询） */
    private Long chatId;

    /** 会话标识 */
    private String sessionId;

    /** 回答正文（含 [1][2] 引用角标） */
    private String answer;

    /** 引用来源列表 */
    private List<RefItem> refs;

    /** 是否命中知识库（false 表示知识库未收录） */
    private boolean hit;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<RefItem> getRefs() {
        return refs;
    }

    public void setRefs(List<RefItem> refs) {
        this.refs = refs;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
    }
}

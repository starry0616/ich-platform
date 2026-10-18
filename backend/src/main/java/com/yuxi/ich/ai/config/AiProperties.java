package com.yuxi.ich.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 问答配置（说明书 4.7 节：模型可配置、可替换）。
 *
 * <p>默认离线模式：不调用外部大模型，直接基于检索到的知识切片
 * 拼装回答（可演示、零成本、不泄露数据）。接入真实模型时：
 * <pre>
 * ich.ai.model.enabled=true
 * ich.ai.model.base-url=https://api.deepseek.com/v1
 * ich.ai.model.api-key=sk-xxxx
 * ich.ai.model.name=deepseek-chat
 * </pre>
 * 接口兼容 OpenAI /chat/completions 的国产模型服务均可接入。
 */
@ConfigurationProperties(prefix = "ich.ai")
public class AiProperties {

    /** 是否启用外部大模型生成；false 时为离线模板回答 */
    private boolean enabled = false;

    /** 大模型接口地址（OpenAI 兼容） */
    private String baseUrl = "https://api.deepseek.com/v1";

    private String apiKey = "";

    private String name = "deepseek-chat";

    /** 游客每分钟提问上限（未登录） */
    private int guestLimitPerMinute = 20;

    /** 检索返回的知识切片上限 */
    private int topK = 3;

    /** 无结果判定阈值（top_score 低于该值视为未命中） */
    private double minScore = 0.3;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGuestLimitPerMinute() {
        return guestLimitPerMinute;
    }

    public void setGuestLimitPerMinute(int guestLimitPerMinute) {
        this.guestLimitPerMinute = guestLimitPerMinute;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }
}

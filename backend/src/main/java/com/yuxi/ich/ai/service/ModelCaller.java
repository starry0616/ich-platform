package com.yuxi.ich.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.ai.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 大模型调用器（OpenAI /chat/completions 兼容接口）。
 *
 * <p>设计文档 4.7.2：仅依据知识库资料片段作答，资料未覆盖时明确说明，
 * 不得凭通用知识推测。调用失败时由调用方降级为离线模板回答。
 */
@Component
public class ModelCaller {

    private static final Logger log = LoggerFactory.getLogger(ModelCaller.class);

    private static final String SYSTEM_PROMPT = """
            你是"非遗文化传承与互动平台"的领域知识助手。
            回答规则：
            1. 仅依据用户提供的"资料片段"作答，不得使用资料之外的通用知识；
            2. 资料未覆盖的问题，直接回答"平台知识库暂未收录该内容"，不要推测；
            3. 引用资料时在句末标注来源编号，如 [1]、[2]，编号对应资料列表顺序；
            4. 回答使用简体中文，条理清晰，控制在 300 字以内。
            """;

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public ModelCaller(AiProperties properties) {
        this.properties = properties;
    }

    /**
     * 调用大模型生成回答。
     *
     * @param context 检索到的资料片段文本
     * @param question 用户问题
     * @return 模型回答；异常时返回 null（调用方降级）
     */
    public String chat(String context, String question) {
        String baseUrl = properties.getBaseUrl().replaceAll("/+$", "");
        String endpoint = baseUrl + "/chat/completions";
        Map<String, Object> body = Map.of(
                "model", properties.getName(),
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content",
                                "资料片段：\n" + context + "\n\n问题：" + question)));
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("模型调用失败 status={} body={}", response.statusCode(),
                        truncate(response.body(), 300));
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String answer = content.asText(null);
            return (answer == null || answer.isBlank()) ? null : answer.trim();
        } catch (Exception e) {
            log.warn("模型调用异常，降级离线回答：{}", e.getMessage());
            return null;
        }
    }

    private String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}

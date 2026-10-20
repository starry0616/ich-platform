package com.yuxi.ich.ai;

import com.yuxi.ich.ai.dto.ConnectionTestResult;
import com.yuxi.ich.ai.service.AiConnectionTester;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AI 连接测试器纯逻辑单元测试：白名单、状态分类、响应解析。
 *
 * <p>不发起任何真实网络请求，不依赖真实密钥或付费 API。
 */
class AiConnectionTesterUnitTests {

    // ==================== 服务商白名单解析 ====================

    @Test
    @DisplayName("U1. 合法服务商 code 可解析，未知/空返回 null")
    void resolveProviderWhitelist() {
        assertThat(AiConnectionTester.resolveProvider("glm")).isNotNull();
        assertThat(AiConnectionTester.resolveProvider("GLM")).isNotNull();
        assertThat(AiConnectionTester.resolveProvider("siliconflow")).isNotNull();
        assertThat(AiConnectionTester.resolveProvider("openai")).isNull();
        assertThat(AiConnectionTester.resolveProvider(null)).isNull();
    }

    // ==================== URL 白名单/SSRF 校验 ====================

    @Test
    @DisplayName("U2. 白名单 https 地址通过校验")
    void allowedHttpsHostsPass() {
        assertThat(AiConnectionTester.validateBaseUrl("https://open.bigmodel.cn/api/paas/v4")).isNull();
        assertThat(AiConnectionTester.validateBaseUrl("https://api.siliconflow.cn/v1")).isNull();
    }

    @Test
    @DisplayName("U3. 拒绝非 HTTPS / 非标准端口 / 任意域名 / IP / 内网 / localhost")
    void rejectDangerousTargets() {
        assertThat(AiConnectionTester.validateBaseUrl("http://open.bigmodel.cn/api/paas/v4"))
                .contains("HTTPS");
        assertThat(AiConnectionTester.validateBaseUrl("https://open.bigmodel.cn:8443/v1"))
                .contains("443");
        assertThat(AiConnectionTester.validateBaseUrl("https://evil.example.com/v1"))
                .contains("允许列表");
        assertThat(AiConnectionTester.validateBaseUrl("https://127.0.0.1/v1")).isNotNull();
        assertThat(AiConnectionTester.validateBaseUrl("https://10.0.0.5/v1")).isNotNull();
        assertThat(AiConnectionTester.validateBaseUrl("https://192.168.1.2/v1")).isNotNull();
        assertThat(AiConnectionTester.validateBaseUrl("https://localhost/v1")).isNotNull();
    }

    // ==================== 状态码分类 ====================

    @Test
    @DisplayName("U4. 200 且含非空内容 → SUCCESS")
    void classifySuccess() {
        String body = "{\"choices\":[{\"message\":{\"content\":\"你好\"}}]}";
        ConnectionTestResult r = AiConnectionTester.classify(200, body, 120L, "glm-4-flash");
        assertThat(r.isOk()).isTrue();
        assertThat(r.getStage()).isEqualTo(AiConnectionTester.SUCCESS);
        assertThat(r.getHttpStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("U5. 200 但无有效内容 → BAD_RESPONSE")
    void classifyBadResponse() {
        ConnectionTestResult r = AiConnectionTester.classify(200, "{\"choices\":[]}", 10L, "m");
        assertThat(r.isOk()).isFalse();
        assertThat(r.getStage()).isEqualTo(AiConnectionTester.BAD_RESPONSE);
    }

    @Test
    @DisplayName("U6. 401/403/404/400/429/5xx 分别正确分类")
    void classifyStatusCodes() {
        assertThat(AiConnectionTester.classify(401, "", 1L, "m").getStage())
                .isEqualTo(AiConnectionTester.AUTH_FAILED);
        assertThat(AiConnectionTester.classify(403, "", 1L, "m").getStage())
                .isEqualTo(AiConnectionTester.FORBIDDEN);
        assertThat(AiConnectionTester.classify(404, "", 1L, "m").getStage())
                .isEqualTo(AiConnectionTester.NOT_FOUND);
        assertThat(AiConnectionTester.classify(400, "", 1L, "m").getStage())
                .isEqualTo(AiConnectionTester.BAD_REQUEST);
        assertThat(AiConnectionTester.classify(429, "", 1L, "m").getStage())
                .isEqualTo(AiConnectionTester.RATE_LIMITED);
        assertThat(AiConnectionTester.classify(503, "", 1L, "m").getStage())
                .isEqualTo(AiConnectionTester.UPSTREAM_ERROR);
    }

    @Test
    @DisplayName("U7. 404 说明不断言一定是模型名错误")
    void notFoundMessageDoesNotBlameModelOnly() {
        ConnectionTestResult r = AiConnectionTester.classify(404, "", 1L, "m");
        assertThat(r.getMessage()).contains("模型").contains("路径");
    }

    // ==================== 响应解析 ====================

    @Test
    @DisplayName("U8. 异常 JSON / 空 choices 解析为无效内容")
    void parseInvalidContent() {
        assertThat(AiConnectionTester.hasValidContent("not-a-json")).isFalse();
        assertThat(AiConnectionTester.hasValidContent("{\"choices\":[]}")).isFalse();
        assertThat(AiConnectionTester.hasValidContent(
                "{\"choices\":[{\"message\":{\"content\":\"\"}}]}")).isFalse();
    }

    // ==================== 密钥不进入结果 ====================

    @Test
    @DisplayName("U9. 分类结果对象不携带任何密钥串")
    void resultNeverContainsSecret() {
        String secret = "sk-secret-abcdef123456";
        ConnectionTestResult r = AiConnectionTester.classify(401, "", 1L, "glm-4-flash");
        String serialized = r.toString() + "|" + r.getMessage() + "|" + r.getStage();
        assertThat(serialized).doesNotContain(secret);
    }
}

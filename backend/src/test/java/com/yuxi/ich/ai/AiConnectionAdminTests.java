package com.yuxi.ich.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.ai.dto.ConnectionTestResult;
import com.yuxi.ich.ai.service.AiConnectionTester;
import com.yuxi.ich.security.TokenBlacklist;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * AI 连接测试接口的权限与响应安全集成测试。
 *
 * <p>用 {@code @MockBean} 替换真实探测器，不发起任何外部 HTTP 请求；
 * 重点验证后端权限边界（未登录 401 / 普通用户 403 / 管理员 200）
 * 以及响应不回显完整 API Key。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AI 连接测试：管理员权限与密钥不回显")
class AiConnectionAdminTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @MockBean
    private AiConnectionTester connectionTester;

    private final ObjectMapper om = new ObjectMapper();

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        tokenBlacklist.clear();
        adminToken = login("admin", "123456");
        userToken = login("user", "123456");
        when(connectionTester.test(any(), any(), any()))
                .thenReturn(ConnectionTestResult.of(true, AiConnectionTester.SUCCESS, 200,
                        "连接成功，模型可正常响应", 88L, "glm-4-flash"));
    }

    @Test
    @DisplayName("P1. 管理员调用连接测试成功(200)")
    void adminAllowed() throws Exception {
        String body = "{\"provider\":\"glm\",\"apiKey\":\"sk-admin-secret-xyz\",\"model\":\"glm-4-flash\"}";
        MvcResult r = mockMvc.perform(post("/api/admin/ai/connection/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        assertThat(json.get("data").get("ok").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("P2. 未登录访问被拒绝(401)")
    void anonymousRejected() throws Exception {
        String body = "{\"provider\":\"glm\",\"apiKey\":\"sk-anon-secret\"}";
        MvcResult r = mockMvc.perform(post("/api/admin/ai/connection/test")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("P3. 普通用户访问被拒绝(403)")
    void plainUserRejected() throws Exception {
        String body = "{\"provider\":\"glm\",\"apiKey\":\"sk-user-secret\"}";
        MvcResult r = mockMvc.perform(post("/api/admin/ai/connection/test")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("P4. 管理员响应体不回显完整 API Key")
    void responseDoesNotLeakKey() throws Exception {
        String secret = "sk-admin-secret-xyz";
        String body = "{\"provider\":\"glm\",\"apiKey\":\"" + secret + "\",\"model\":\"glm-4-flash\"}";
        MvcResult r = mockMvc.perform(post("/api/admin/ai/connection/test")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn();
        String content = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(content).doesNotContain(secret);
    }

    private String login(String username, String password) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        return json.get("data").get("accessToken").asText();
    }
}

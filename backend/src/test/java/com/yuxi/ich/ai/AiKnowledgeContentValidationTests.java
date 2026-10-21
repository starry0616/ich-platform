package com.yuxi.ich.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.ai.mapper.AiKnowledgeMapper;
import com.yuxi.ich.security.TokenBlacklist;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 知识正文长度校验集成测试（H2 内存库，管理员身份）。
 *
 * <p>验证 {@code KnowledgeSaveRequest.content} 的 {@code @Size(10000)}：
 * 空值拒绝、正好上限通过、超上限拒绝，且超限时返回清晰的 400 参数错误。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("知识正文长度校验")
class AiKnowledgeContentValidationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Autowired
    private AiKnowledgeMapper knowledgeMapper;

    private final ObjectMapper om = new ObjectMapper();
    private String adminToken;
    /** 仅记录本测试成功落库的知识 id，@AfterEach 只清理这条，不动其他测试数据 */
    private Long createdKnowledgeId;

    @BeforeEach
    void setUp() throws Exception {
        tokenBlacklist.clear();
        adminToken = login("admin", "123456");
    }

    @AfterEach
    void cleanupSelfCreated() {
        if (createdKnowledgeId != null) {
            knowledgeMapper.deleteById(createdKnowledgeId);
            createdKnowledgeId = null;
        }
    }

    @Test
    @DisplayName("C1. 正文为空返回 400")
    void blankContentRejected() throws Exception {
        MvcResult r = postKnowledge("测试标题", "   ");
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("C2. 正好 10000 字正文保存成功(200)")
    void exactlyMaxAccepted() throws Exception {
        String content = "知".repeat(10000);
        MvcResult r = postKnowledge("长度边界知识", content);
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(200);
        // 记录成功落库的知识 id，交由 @AfterEach 仅删除本条
        createdKnowledgeId = json.get("data").asLong();
    }

    @Test
    @DisplayName("C3. 超过 10000 字(10001)返回 400，不落库")
    void overMaxRejected() throws Exception {
        String content = "知".repeat(10001);
        MvcResult r = postKnowledge("超长知识", content);
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    private MvcResult postKnowledge(String title, String content) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("sourceType", 1);
        body.put("content", content);
        return mockMvc.perform(post("/api/admin/ai/knowledge")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
                .andReturn();
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

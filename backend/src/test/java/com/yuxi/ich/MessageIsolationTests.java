package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.message.entity.SysMessage;
import com.yuxi.ich.message.mapper.SysMessageMapper;
import com.yuxi.ich.user.mapper.SysUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 站内通知数据隔离测试（D1）。
 *
 * <p>验证：用户只能标记自己的消息已读，不能操作他人消息(403)；消息列表按本人隔离。
 * 仅用 H2 内存库，临时用户与临时消息用后即删，不连接 MySQL。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("站内通知已读隔离测试")
class MessageIsolationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysMessageMapper messageMapper;

    private final ObjectMapper om = new ObjectMapper();
    private final List<Long> tempUserIds = new ArrayList<>();
    private final List<Long> tempMessageIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempMessageIds) {
            messageMapper.deleteById(id);
        }
        tempMessageIds.clear();
        for (Long id : tempUserIds) {
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
    }

    @Test
    @DisplayName("他人消息不能标记已读(403)；本人消息可标记已读(200)；列表只见本人消息")
    void messageIsolatedPerUser() throws Exception {
        String nameA = "msg_a_" + System.nanoTime();
        String nameB = "msg_b_" + System.nanoTime();
        Long idA = registerUser(nameA);
        registerUser(nameB);
        String tokenA = login(nameA);
        String tokenB = login(nameB);

        // 给 A 造一条未读消息
        SysMessage msg = new SysMessage();
        msg.setUserId(idA);
        msg.setMsgType(1);
        msg.setTitle("系统通知");
        msg.setContent("隔离测试消息");
        msg.setIsRead(0);
        messageMapper.insert(msg);
        tempMessageIds.add(msg.getId());

        // B 尝试标记 A 的消息已读 → 403
        MvcResult forbidden = mockMvc.perform(put("/api/message/" + msg.getId() + "/read")
                .header("Authorization", "Bearer " + tokenB)).andReturn();
        assertThat(body(forbidden).get("code").asInt()).isEqualTo(403);

        // A 自己标记已读 → 200
        MvcResult ok = mockMvc.perform(put("/api/message/" + msg.getId() + "/read")
                .header("Authorization", "Bearer " + tokenA)).andReturn();
        assertThat(body(ok).get("code").asInt()).isEqualTo(200);

        // B 的消息列表不含 A 的消息
        JsonNode bList = body(mockMvc.perform(get("/api/message/list")
                .header("Authorization", "Bearer " + tokenB)).andReturn());
        JsonNode records = bList.get("data").get("records");
        boolean containsA = false;
        if (records != null && records.isArray()) {
            for (JsonNode row : records) {
                JsonNode rid = row.get("id");
                if (rid != null && rid.asLong() == msg.getId()) {
                    containsA = true;
                }
            }
        }
        assertThat(containsA).isFalse();
    }

    // ==================== HTTP 辅助 ====================

    private Long registerUser(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Abc12345\","
                        + "\"nickname\":\"" + username + "\"}")).andReturn();
        JsonNode b = body(r);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        Long id = b.get("data").asLong();
        tempUserIds.add(id);
        return id;
    }

    private String login(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Abc12345\"}"))
                .andReturn();
        JsonNode b = body(r);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    private JsonNode body(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.security.TokenBlacklist;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 认证会话安全测试（第 13—16 批）。
 *
 * <p>A1：登出同时撤销 access 与 refresh token，旧 refresh 不得再换新 access；
 * 重复登出、缺令牌不报错。A2：管理员用户列表不回传完整证件号。
 *
 * <p>仅用 H2 内存库与演示账号，不连接 MySQL；A2 临时用户用后即删。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("认证会话安全测试")
class AuthSessionSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper om = new ObjectMapper();
    private final List<Long> tempUserIds = new ArrayList<>();

    @BeforeEach
    void reset() {
        tokenBlacklist.clear();
        // 清除跨方法残留的认证上下文，避免无令牌请求被误判为已登录
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        // 删除本类临时插入的用户，避免污染共享内存库
        for (Long id : tempUserIds) {
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
    }

    // ==================== A1. 登出撤销 refresh token ====================

    @Test
    @DisplayName("A1a. 登出后旧 refresh token 刷新被拒(401)，合法刷新在登出前可用")
    void logoutRevokesRefreshToken() throws Exception {
        JsonNode login = login("user");
        String refresh = login.get("data").get("refreshToken").asText();
        String access = login.get("data").get("accessToken").asText();

        // 登出前：合法 refresh 可换取新 access
        JsonNode before = refresh(refresh);
        assertThat(before.get("code").asInt()).isEqualTo(200);
        assertThat(before.get("data").get("accessToken").asText()).isNotBlank();

        // 登出（query 传 refresh，Authorization 头传 access）
        MvcResult out = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .param("refreshToken", refresh)).andReturn();
        JsonNode outJson = om.readTree(out.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(outJson.get("code").asInt()).isEqualTo(200);

        // 登出后：旧 refresh 不得再换新 access
        JsonNode after = refresh(refresh);
        assertThat(after.get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("A1b. 无令牌登出被安全层拒绝(body.code=401)；有效 access 登出成功(200)")
    void logoutRequiresValidToken() throws Exception {
        // 无任何令牌：安全层拒绝，HTTP 200 但 body.code=401（项目统一错误码约定）
        MvcResult r1 = mockMvc.perform(post("/api/auth/logout")).andReturn();
        JsonNode b1 = om.readTree(r1.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b1.get("code").asInt()).isEqualTo(401);

        JsonNode login = login("user");
        String access = login.get("data").get("accessToken").asText();
        // 带有效 access、无 refresh 登出成功
        MvcResult r2 = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)).andReturn();
        assertThat(om.readTree(r2.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("A1c. 登出前旧 access 可访问受保护接口；登出后同一旧 access 被拒(401)")
    void accessTokenRevokedAfterLogout() throws Exception {
        JsonNode login = login("user");
        String access = login.get("data").get("accessToken").asText();
        String refresh = login.get("data").get("refreshToken").asText();

        // 登出前：合法 access 访问个人资料成功
        MvcResult before = mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + access)).andReturn();
        assertThat(om.readTree(before.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("code").asInt()).isEqualTo(200);

        // 登出（access 走 Authorization 头，refresh 走 query）
        MvcResult out = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .param("refreshToken", refresh)).andReturn();
        assertThat(om.readTree(out.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("code").asInt()).isEqualTo(200);

        // 登出后：同一旧 access 访问受保护接口被拒(401)
        MvcResult after = mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + access)).andReturn();
        assertThat(om.readTree(after.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("A1d. 伪造随机串与错类型(access 当 refresh)令牌刷新均被拒(401)")
    void forgedAndWrongTypeRefreshRejected() throws Exception {
        // 随机伪造串
        JsonNode forged = refresh("not-a-real-jwt-" + System.nanoTime());
        assertThat(forged.get("code").asInt()).isEqualTo(401);

        // 用合法 access token 冒充 refresh token（类型错误）
        JsonNode login = login("user");
        String access = login.get("data").get("accessToken").asText();
        JsonNode wrongType = refresh(access);
        assertThat(wrongType.get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("A1e. 重复登出不抛未处理异常，且不恢复会话（第二次 200/401 均可接受）")
    void repeatedLogoutIsIdempotent() throws Exception {
        JsonNode login = login("user");
        String access = login.get("data").get("accessToken").asText();
        String refresh = login.get("data").get("refreshToken").asText();

        MvcResult first = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .param("refreshToken", refresh)).andReturn();
        assertThat(om.readTree(first.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("code").asInt()).isEqualTo(200);

        // 第二次重复登出：不抛未处理异常。此时 access 已进黑名单，安全层可能返回
        // 200（服务层静默跳过）或 401（安全层拒绝已拉黑令牌），二者都不算错误
        MvcResult second = mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + access)
                .param("refreshToken", refresh)).andReturn();
        int secondCode = om.readTree(second.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("code").asInt();
        assertThat(secondCode).isIn(200, 401);

        // 会话未被恢复：旧 refresh 仍不可换新 access
        assertThat(refresh(refresh).get("code").asInt()).isEqualTo(401);
    }

    // ==================== A2. 管理员列表证件号脱敏 ====================

    @Test
    @DisplayName("A2. 管理员用户列表不返回完整身份证号，仅返回脱敏串")
    void adminListMasksIdCard() throws Exception {
        String fullId = "530102199001011234";
        SysUser tmp = new SysUser();
        tmp.setUsername("maskcheck_" + System.nanoTime());
        tmp.setPassword("x");
        tmp.setNickname("证件脱敏检查");
        tmp.setRoleId(1L);
        tmp.setStatus(1);
        tmp.setIdCard(fullId);
        userMapper.insert(tmp);
        tempUserIds.add(tmp.getId());

        String adminToken = login("admin").get("data").get("accessToken").asText();
        MvcResult r = mockMvc.perform(get("/api/admin/user/list")
                .header("Authorization", "Bearer " + adminToken)
                .param("keyword", tmp.getUsername())).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = om.readTree(body);
        assertThat(json.get("code").asInt()).isEqualTo(200);

        // 完整证件号不得出现在响应中；脱敏串（前3 + 后3）必须出现
        assertThat(body).doesNotContain(fullId);
        assertThat(body).contains("530********234");
    }

    @Test
    @DisplayName("A2b. 管理员用户列表不返回完整手机号，仅返回脱敏串")
    void adminListMasksPhone() throws Exception {
        String fullPhone = "13911112222";
        SysUser tmp = new SysUser();
        tmp.setUsername("phonemask_" + System.nanoTime());
        tmp.setPassword("x");
        tmp.setNickname("手机脱敏检查");
        tmp.setRoleId(1L);
        tmp.setStatus(1);
        tmp.setPhone(fullPhone);
        userMapper.insert(tmp);
        tempUserIds.add(tmp.getId());

        String adminToken = login("admin").get("data").get("accessToken").asText();
        MvcResult r = mockMvc.perform(get("/api/admin/user/list")
                .header("Authorization", "Bearer " + adminToken)
                .param("keyword", tmp.getUsername())).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = om.readTree(body);
        assertThat(json.get("code").asInt()).isEqualTo(200);
        // 完整手机号不得出现在响应中；脱敏串（前3 + **** + 后4）必须出现
        assertThat(body).doesNotContain(fullPhone);
        assertThat(body).contains("139****2222");
    }

    @Test
    @DisplayName("E2. 个人资料接口返回脱敏证件号，不泄露完整号与密码字段")
    void profileMasksIdCard() throws Exception {
        String fullId = "530102199001011234";
        String uname = "prof_mask_" + System.nanoTime();
        SysUser tmp = new SysUser();
        tmp.setUsername(uname);
        tmp.setPassword(passwordEncoder.encode("Abc12345"));
        tmp.setNickname("资料脱敏检查");
        tmp.setRoleId(1L);
        tmp.setStatus(1);
        tmp.setIdCard(fullId);
        userMapper.insert(tmp);
        tempUserIds.add(tmp.getId());

        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + uname + "\",\"password\":\"Abc12345\"}")).andReturn();
        JsonNode loginJson = om.readTree(login.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(loginJson.get("code").asInt()).isEqualTo(200);
        String access = loginJson.get("data").get("accessToken").asText();

        MvcResult r = mockMvc.perform(get("/api/user/profile")
                .header("Authorization", "Bearer " + access)).andReturn();
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = om.readTree(body);
        assertThat(json.get("code").asInt()).isEqualTo(200);
        // 完整证件号与密码字段不得出现，脱敏串必须出现
        assertThat(body).doesNotContain(fullId);
        assertThat(body).contains("530********234");
        assertThat(body).doesNotContain("\"password\"");
    }

    // ==================== 工具方法 ====================

    private JsonNode login(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andReturn();
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/refresh")
                .param("refreshToken", refreshToken)).andReturn();
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

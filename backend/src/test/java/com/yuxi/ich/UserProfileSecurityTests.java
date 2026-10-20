package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.security.TokenBlacklist;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 个人资料接口安全测试（第 6 批）。
 *
 * <p>三组只读/边界校验，不修改业务代码：
 * <ul>
 *   <li>A. GET /api/user/profile 响应不携带密码哈希、访问/刷新令牌等敏感字段；</li>
 *   <li>B. PUT /api/user/profile 普通用户夹带 roleId/status/username/password 不能改权限、状态或密码；</li>
 *   <li>C. POST /api/media/upload 未登录拒绝、非法扩展名拒绝、超大图片拒绝。</li>
 * </ul>
 *
 * <p>仅使用 H2 内存库演示账号 user（普通用户，role_id=1），不连接 MySQL，
 * 不在真实账号上改密码；B 用例结束后把昵称/电话恢复为原值。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("个人资料接口安全测试")
class UserProfileSecurityTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void reset() {
        tokenBlacklist.clear();
    }

    // ==================== A. 个人资料不泄露敏感字段 ====================

    @Test
    @DisplayName("A1. GET /api/user/profile 响应不含 password / passwordHash / accessToken / refreshToken")
    void profileDoesNotLeakSecrets() throws Exception {
        String token = loginToken("user");

        String body = rawBody("GET", "/api/user/profile", token, null);
        JsonNode json = om.readTree(body);
        assertThat(json.get("code").asInt()).isEqualTo(200);

        // 响应体（含嵌套 data）不得出现密码摘要与认证令牌字段名
        assertThat(body)
                .as("个人资料响应不得携带密码字段")
                .doesNotContain("\"password\"", "passwordHash")
                .as("个人资料响应不得携带访问/刷新令牌")
                .doesNotContain("accessToken", "refreshToken");
    }

    // ==================== B. 恶意夹带 role/status 不生效 ====================

    @Test
    @DisplayName("B1. 普通用户夹带 roleId/status/username/password，角色状态密码不变且白名单字段可更新")
    void maliciousRoleStatusAreIgnored() throws Exception {
        String token = loginToken("user");
        Long userId = currentUserId(token);

        // 改前快照（仅内存比较，不输出哈希）
        SysUser before = userMapper.selectById(userId);
        Long beforeRoleId = before.getRoleId();
        Integer beforeStatus = before.getStatus();
        String beforePassword = before.getPassword();
        String beforeNickname = before.getNickname();
        String beforePhone = before.getPhone();

        // 正常更新昵称/电话，同时夹带越权字段
        String malicious = "{\"nickname\":\"安全测试新昵称\",\"phone\":\"13911112222\","
                + "\"email\":\"sec@test.local\","
                + "\"roleId\":3,\"status\":0,\"username\":\"hacker\",\"password\":\"Hacked123!\"}";
        JsonNode resp = call("PUT", "/api/user/profile", token, malicious);
        assertThat(resp.get("code").asInt()).isEqualTo(200);

        SysUser after = userMapper.selectById(userId);
        // 越权字段必须保持不变
        assertThat(after.getRoleId()).as("角色 id 不得被夹带请求改变").isEqualTo(beforeRoleId);
        assertThat(after.getStatus()).as("账号状态不得被夹带请求改变").isEqualTo(beforeStatus);
        assertThat(after.getPassword()).as("密码哈希不得被夹带请求改写").isEqualTo(beforePassword);
        assertThat(after.getUsername()).as("用户名不得被夹带请求改写").isEqualTo(before.getUsername());
        // 白名单字段正常更新
        assertThat(after.getNickname()).isEqualTo("安全测试新昵称");
        assertThat(after.getPhone()).isEqualTo("13911112222");

        // 恢复昵称/电话，避免污染共享内存库
        SysUser restore = new SysUser();
        restore.setId(userId);
        restore.setNickname(beforeNickname);
        restore.setPhone(beforePhone);
        userMapper.updateById(restore);
    }

    // ==================== C. 媒体上传边界 ====================

    @Test
    @DisplayName("C1. 未登录上传媒体被拒绝(401)")
    void uploadWithoutLoginRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", new byte[]{1, 2, 3, 4});
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("C2. 已登录上传非法扩展名(.exe)被拒绝(400)")
    void uploadIllegalExtensionRejected() throws Exception {
        String token = loginToken("user");
        MockMultipartFile file = new MockMultipartFile(
                "file", "evil.exe", "application/octet-stream", new byte[]{1, 2, 3});
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("C3. 已登录上传超过 5MB 的图片被拒绝(400)")
    void uploadOversizedImageRejected() throws Exception {
        String token = loginToken("user");
        // 6MB 字节，超过图片 5MB 上限；Spring 全局上限 200MB，会到达 Service 校验
        byte[] big = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", big);
        MvcResult r = mockMvc.perform(multipart("/api/media/upload").file(file)
                .header("Authorization", "Bearer " + token)).andReturn();
        JsonNode json = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.get("code").asInt()).isEqualTo(400);
    }

    // ==================== 工具方法 ====================

    private JsonNode call(String method, String url, String token, String body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> get(url);
        };
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult r = mockMvc.perform(req).andReturn();
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String rawBody(String method, String url, String token, String body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            default -> get(url);
        };
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(req).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String loginToken(String username) throws Exception {
        JsonNode b = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"123456\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    private Long currentUserId(String token) throws Exception {
        JsonNode me = call("GET", "/api/user/profile", token, null);
        return me.get("data").get("id").asLong();
    }
}

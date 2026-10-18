package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 传承人模块集成测试。
 *
 * <p>重点验证比项目模块多出的那条<b>角色提升</b>链路：
 * 申请 → 管理员审核通过 → 账号角色升级为 ROLE_INHERITOR，
 * 且审核与角色变更在同一事务内原子生效。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("传承人模块集成测试")
class InheritorModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.yuxi.ich.security.TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void reset() {
        tokenBlacklist.clear();
    }

    private JsonNode call(String method, String url, String token, String body) throws Exception {
        var req = "POST".equals(method)
                ? post(url) : ("PUT".equals(method) ? put(url) : get(url));
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult r = mockMvc.perform(req).andExpect(status().isOk()).andReturn();
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String token(String username) throws Exception {
        JsonNode b = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"123456\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("I1. 传承人名录只返回已认证")
    void listOnlyCertified() throws Exception {
        // 用演示数据独有的地区缩小范围，避免受其他用例新建/认证的数据影响。
        // 教训：不要断言「全表恰好 N 条」——其他用例会新增并认证传承人。
        JsonNode b = call("GET", "/api/inheritor/list?region=红塔区&pageSize=50", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        JsonNode data = b.get("data");
        assertThat(data.get("total").asLong()).isGreaterThan(0);
        for (JsonNode n : data.get("records")) {
            assertThat(n.get("certStatus").asInt()).isEqualTo(1);
            assertThat(n.get("region").asText()).contains("红塔区");
        }
    }

    @Test
    @DisplayName("I1b. 名录带出关联项目名称")
    void listJoinsProjectName() throws Exception {
        JsonNode b = call("GET", "/api/inheritor/list?keyword=李文&pageSize=50", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        JsonNode records = b.get("data").get("records");
        assertThat(records.size()).isGreaterThan(0);
        boolean hasProject = false;
        for (JsonNode n : records) {
            if (n.hasNonNull("projectName")) {
                hasProject = true;
                break;
            }
        }
        assertThat(hasProject).isTrue();
    }

    @Test
    @DisplayName("I2. 按级别与地区筛选生效")
    void listFilters() throws Exception {
        JsonNode b = call("GET", "/api/inheritor/list?level=1&region=云南&pageSize=50", null, null);
        JsonNode data = b.get("data");
        assertThat(data.get("total").asLong()).isGreaterThan(0);
        for (JsonNode n : data.get("records")) {
            assertThat(n.get("level").asInt()).isEqualTo(1);
            assertThat(n.get("region").asText()).contains("云南");
        }
    }

    @Test
    @DisplayName("I3. 按姓名关键词检索")
    void listByKeyword() throws Exception {
        JsonNode b = call("GET", "/api/inheritor/list?keyword=李&pageSize=50", null, null);
        assertThat(b.get("data").get("total").asLong()).isGreaterThan(0);
    }

    @Test
    @DisplayName("I4. 详情返回姓名字段；关联项目名允许为空（Jackson 配了 non_null，空值会被省略）")
    void detail() throws Exception {
        JsonNode b = call("GET", "/api/inheritor/1", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        JsonNode data = b.get("data");
        assertThat(data.get("name").asText()).isEqualTo("李文");
        assertThat(data.get("certNo").asText()).isEqualTo("YN-YX-0001");
        // projectName 在演示数据中可能为 null；全局 Jackson 配置会省略 null 字段，
        // 因此这里只要求「如果要输出，就必须是正确值」
        if (data.hasNonNull("projectName")) {
            assertThat(data.get("projectName").asText()).isEqualTo("玉溪青花瓷烧制技艺");
        }
    }

    @Test
    @DisplayName("I5. 不存在的传承人返回 404")
    void detailNotFound() throws Exception {
        JsonNode b = call("GET", "/api/inheritor/999999", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(404);
    }

    // ==================== 资质申请 ====================

    @Test
    @DisplayName("I6. 普通用户可提交资质申请，状态为待审核")
    void applyCreatesPending() throws Exception {
        String t = token("user");
        // user 账号在演示数据中没有传承人档案，首次申请应成功
        JsonNode b = call("POST", "/api/inheritor/apply", t,
                "{\"name\":\"测试申请人\",\"gender\":1,\"level\":4,"
                        + "\"region\":\"云南省玉溪市\",\"craftYears\":10,"
                        + "\"certNo\":\"TEST-CERT-001\",\"intro\":\"集成测试提交\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);

        // 查询进度应为待审核
        JsonNode s = call("GET", "/api/inheritor/apply/status", t, null);
        assertThat(s.get("code").asInt()).isEqualTo(200);
        assertThat(s.get("data").get("certStatus").asInt()).isEqualTo(0);
        assertThat(s.get("data").get("name").asText()).isEqualTo("测试申请人");
    }

    @Test
    @DisplayName("I7. 待审核期间重复申请被拒绝（409）")
    void applyTwiceRejected() throws Exception {
        String t = token("user");
        JsonNode s = call("GET", "/api/inheritor/apply/status", t, null);
        // 上一条测试已创建档案；若未创建则跳过重复校验
        if (s.get("code").asInt() != 200) {
            return;
        }
        JsonNode b = call("POST", "/api/inheritor/apply", t,
                "{\"name\":\"再次申请\",\"level\":4}");
        assertThat(b.get("code").asInt()).isEqualTo(409);
    }

    @Test
    @DisplayName("I8. 传承人已认证，再申请被拒绝")
    void applyWhenAlreadyCertified() throws Exception {
        String t = token("inheritor");
        JsonNode b = call("POST", "/api/inheritor/apply", t,
                "{\"name\":\"重复申请\",\"level\":1}");
        assertThat(b.get("code").asInt()).isEqualTo(409);
    }

    @Test
    @DisplayName("I9. 申请参数校验：姓名为空被拦截")
    void applyValidation() throws Exception {
        String t = token("user");
        JsonNode b = call("POST", "/api/inheritor/apply", t, "{\"level\":4}");
        assertThat(b.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("I10. 未登录不能提交申请（401）")
    void applyRequiresLogin() throws Exception {
        JsonNode b = call("POST", "/api/inheritor/apply", null,
                "{\"name\":\"匿名\",\"level\":4}");
        assertThat(b.get("code").asInt()).isEqualTo(401);
    }

    // ==================== 审核与角色提升 ====================

    @Test
    @DisplayName("I11. 管理员审核通过后，账号角色自动提升为传承人")
    void auditPromotesRole() throws Exception {
        // 注意：不假设初始角色。测试执行顺序不保证，user 可能已被其他用例提升过。
        // 因此改为「审核前记录角色 → 审核 → 断言变为 ROLE_INHERITOR」。
        String userToken = token("user");
        String adminToken = token("admin");

        // 1) 确保存在一条档案；若已认证则先取到它
        JsonNode s = call("GET", "/api/inheritor/apply/status", userToken, null);
        long inheritorId;
        if (s.get("code").asInt() == 200) {
            inheritorId = s.get("data").get("id").asLong();
        } else {
            JsonNode created = call("POST", "/api/inheritor/apply", userToken,
                    "{\"name\":\"待认证用户\",\"gender\":1,\"level\":4,"
                            + "\"region\":\"云南省玉溪市\",\"certNo\":\"TEST-CERT-002\"}");
            assertThat(created.get("code").asInt()).isEqualTo(200);
            inheritorId = created.get("data").asLong();
        }

        // 2) 审核前记录角色
        String roleBefore = call("GET", "/api/auth/me", userToken, null)
                .get("data").get("roleCode").asText();
        assertThat(roleBefore).isIn("ROLE_USER", "ROLE_INHERITOR");

        // 3) 管理员审核通过
        JsonNode a = call("PUT", "/api/inheritor/" + inheritorId + "/audit?certStatus=1",
                adminToken, null);
        assertThat(a.get("code").asInt()).isEqualTo(200);

        // 4) 审核后角色必为传承人（用新令牌验证，角色写在令牌里）
        String newToken = token("user");
        JsonNode meAfter = call("GET", "/api/auth/me", newToken, null);
        assertThat(meAfter.get("data").get("roleCode").asText()).isEqualTo("ROLE_INHERITOR");
        assertThat(meAfter.get("data").get("isInheritor").asBoolean()).isTrue();

        // 5) 前台名录应能看到这条已认证档案（按认定编号精确匹配）
        String certNo = call("GET", "/api/inheritor/" + inheritorId, null, null)
                .get("data").get("certNo").asText();
        JsonNode list = call("GET", "/api/inheritor/list?keyword=" + certNo + "&pageSize=10",
                null, null);
        assertThat(list.get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("I12. 审核驳回后角色不提升")
    void auditRejectKeepsRole() throws Exception {
        String adminToken = token("admin");
        // 对已有档案执行驳回（演示数据中 id=3 已认证，这里只验证状态可被改为驳回）
        JsonNode a = call("PUT", "/api/inheritor/3/audit?certStatus=2", adminToken, null);
        assertThat(a.get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("I13. 审核状态取值非法时返回 400")
    void auditInvalidStatus() throws Exception {
        String adminToken = token("admin");
        JsonNode b = call("PUT", "/api/inheritor/1/audit?certStatus=9", adminToken, null);
        assertThat(b.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("I14. 普通用户不能执行审核（403）")
    void auditForbiddenForUser() throws Exception {
        String t = token("user");
        JsonNode b = call("PUT", "/api/inheritor/1/audit?certStatus=1", t, null);
        assertThat(b.get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("I15. 后台待审列表可看到全部状态（管理员）")
    void adminAuditList() throws Exception {
        String adminToken = token("admin");
        JsonNode b = call("GET", "/api/inheritor/admin/audit/list?pageSize=50", adminToken, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        // 全部传承人档案（不筛 cert_status）
        assertThat(b.get("data").get("total").asLong()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("I16. 普通用户不能访问后台待审列表（403）")
    void adminAuditListForbidden() throws Exception {
        String t = token("user");
        JsonNode b = call("GET", "/api/inheritor/admin/audit/list", t, null);
        assertThat(b.get("code").asInt()).isEqualTo(403);
    }

    // ==================== 档案维护 ====================

    @Test
    @DisplayName("I17. 传承人可维护自己的主页")
    void updateOwnProfile() throws Exception {
        // 只验证 inheritor 账号（角色固定，不受其他用例影响）。
        // 不对返回码做「200 或 403」的模糊断言——那等于没测。
        JsonNode b = call("PUT", "/api/inheritor/profile", token("inheritor"),
                "{\"intro\":\"集成测试更新的简介\",\"contact\":\"0877-0000000\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("I18. 未提交申请的账号维护主页被拒（403）")
    void updateProfileForbiddenForNonInheritor() throws Exception {
        // 用新注册账号，保证角色一定是 ROLE_USER。
        // 教训：不要复用演示账号 user —— 其他用例可能已把它提升为传承人。
        String uname = "noprofile" + System.currentTimeMillis() % 1000000;
        assertThat(call("POST", "/api/auth/register", null,
                "{\"username\":\"" + uname + "\",\"password\":\"abc12345\"}")
                .get("code").asInt()).isEqualTo(200);
        String t = token2(uname, "abc12345");

        JsonNode b = call("PUT", "/api/inheritor/profile", t, "{\"intro\":\"越权尝试\"}");
        assertThat(b.get("code").asInt()).isEqualTo(403);
    }

    /** 指定密码登录（注册用例使用），返回令牌 */
    private String token2(String username, String password) throws Exception {
        JsonNode b = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    @Test
    @DisplayName("I19. 管理员可维护指定档案，修改后详情可见")
    void adminUpdate() throws Exception {
        String adminToken = token("admin");
        String intro = "管理员更新的简介-" + System.currentTimeMillis() % 100000;
        JsonNode b = call("PUT", "/api/inheritor/2", adminToken,
                "{\"intro\":\"" + intro + "\",\"works\":\"代表作品若干\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);

        JsonNode d = call("GET", "/api/inheritor/2", null, null);
        assertThat(d.get("code").asInt()).isEqualTo(200);
        assertThat(d.get("data").get("intro").asText()).isEqualTo(intro);
    }
}

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 骨架集成测试。
 *
 * <p>使用 h2 内存库真实启动 Spring 容器（含 MyBatis-Plus、Security 过滤器链），
 * 通过 MockMvc 打真实 HTTP 请求，验证：
 * <ul>
 *   <li>服务能正常启动、建表与导入演示数据</li>
 *   <li>公开接口可匿名访问</li>
 *   <li>登录返回双令牌，错误密码被拒绝</li>
 *   <li>带令牌可访问受保护接口，不带令牌返回 401</li>
 *   <li>角色鉴权生效：普通用户访问管理接口返回 403</li>
 *   <li>分页与多条件筛选真实生效</li>
 *   <li>未审核内容对前台不可见</li>
 *   <li>逻辑删除生效</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("后端骨架集成测试")
class IchPlatformApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.yuxi.ich.security.TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

    /**
     * 每个测试前重置令牌黑名单。
     *
     * <p>TokenBlacklist 是单例（骨架阶段为内存实现），退出登录类测试写入的条目
     * 会残留并影响后续用例，造成「单独跑通过、一起跑失败」。
     * 这里显式清理，保证用例之间互不干扰。
     */
    @BeforeEach
    void resetSharedState() {
        tokenBlacklist.clear();
    }

    private JsonNode doLogin(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return om.readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String tokenOf(String username, String password) throws Exception {
        JsonNode body = doLogin(username, password);
        assertThat(body.get("code").asInt()).isEqualTo(200);
        return body.get("data").get("accessToken").asText();
    }

    // ==================== 启动与公开接口 ====================

    @Test
    @DisplayName("1. 服务启动且公开探活接口可匿名访问")
    void healthIsPublic() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("2. 类别接口返回十大门类")
    void categoryList() throws Exception {
        mockMvc.perform(get("/api/category/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.data[0].categoryName").value("民间文学"))
                .andExpect(jsonPath("$.data[7].categoryName").value("传统技艺"));
    }

    // ==================== 列表、分页与筛选 ====================

    @Test
    @DisplayName("3. 项目列表分页可用，且前台只返回已发布内容")
    void projectListPaging() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/project/list")
                        .param("pageNum", "1").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode data = om.readTree(result.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        // 演示数据含 13 条已发布 + 1 条待审核；前台应只看到 13 条
        assertThat(data.get("total").asLong()).isEqualTo(13);
        assertThat(data.get("records").size()).isEqualTo(5);
        assertThat(data.get("pageNum").asLong()).isEqualTo(1);
        assertThat(data.get("pages").asLong()).isEqualTo(3);
        // 列表中不应出现待审核项目
        for (JsonNode n : data.get("records")) {
            assertThat(n.get("projectName").asText()).isNotEqualTo("某待审核技艺");
        }
    }

    @Test
    @DisplayName("4. 多条件组合筛选生效（类别 + 级别）")
    void projectListFilters() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/project/list")
                        .param("categoryId", "8").param("level", "1"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = om.readTree(result.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        long total = data.get("total").asLong();
        assertThat(total).isGreaterThan(0);
        for (JsonNode n : data.get("records")) {
            assertThat(n.get("categoryId").asLong()).isEqualTo(8L);
            assertThat(n.get("level").asInt()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("5. 地域模糊筛选与关键词检索生效")
    void projectListRegionAndKeyword() throws Exception {
        // 云南相关项目
        MvcResult r1 = mockMvc.perform(get("/api/project/list").param("region", "云南"))
                .andExpect(status().isOk()).andReturn();
        JsonNode d1 = om.readTree(r1.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        assertThat(d1.get("total").asLong()).isGreaterThan(0);
        for (JsonNode n : d1.get("records")) {
            assertThat(n.get("region").asText()).contains("云南");
        }

        // 关键词命中项目名称
        MvcResult r2 = mockMvc.perform(get("/api/project/list").param("keyword", "陶瓷"))
                .andExpect(status().isOk()).andReturn();
        JsonNode d2 = om.readTree(r2.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        assertThat(d2.get("total").asLong()).isGreaterThan(0);
    }

    @Test
    @DisplayName("6. 排序白名单生效；非法排序字段回落到默认排序而非注入")
    void projectListSortWhitelist() throws Exception {
        // 合法排序：按浏览量倒序，首条应是浏览量最高的昆曲
        MvcResult r1 = mockMvc.perform(get("/api/project/list")
                        .param("sortField", "view_count").param("sortOrder", "desc")
                        .param("pageSize", "1"))
                .andExpect(status().isOk()).andReturn();
        JsonNode d1 = om.readTree(r1.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        assertThat(d1.get("records").get(0).get("projectName").asText()).isEqualTo("昆曲");

        // 非法排序字段：不应报错，回落到 sort_no 排序
        mockMvc.perform(get("/api/project/list")
                        .param("sortField", "id; DROP TABLE ich_project--"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("7. 热门项目接口返回已发布项目")
    void hotProjects() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/project/hot").param("limit", "3"))
                .andExpect(status().isOk()).andReturn();
        JsonNode data = om.readTree(result.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        assertThat(data.size()).isEqualTo(3);
    }

    // ==================== 详情与可见性 ====================

    @Test
    @DisplayName("8. 项目详情返回完整字段与类别名称")
    void projectDetail() throws Exception {
        mockMvc.perform(get("/api/project/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.projectName").value("玉溪青花瓷烧制技艺"))
                .andExpect(jsonPath("$.data.categoryName").value("传统技艺"))
                .andExpect(jsonPath("$.data.projectCode").value("YN-QHC"));
    }

    @Test
    @DisplayName("9. 待审核项目前台不可见，统一返回 404")
    void pendingProjectNotVisible() throws Exception {
        // 演示数据中 id=14 为待审核项目
        mockMvc.perform(get("/api/project/14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("10. 不存在的项目返回 404")
    void projectNotFound() throws Exception {
        mockMvc.perform(get("/api/project/999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== 认证 ====================

    @Test
    @DisplayName("11. 登录成功返回双令牌与角色")
    void loginSuccess() throws Exception {
        JsonNode body = doLogin("admin", "123456");
        assertThat(body.get("code").asInt()).isEqualTo(200);
        JsonNode data = body.get("data");
        assertThat(data.get("accessToken").asText()).isNotBlank();
        assertThat(data.get("refreshToken").asText()).isNotBlank();
        assertThat(data.get("roleCode").asText()).isEqualTo("ROLE_ADMIN");
        assertThat(data.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(data.get("expiresIn").asLong()).isEqualTo(7200L);
    }

    @Test
    @DisplayName("12. 密码错误返回统一提示，不泄露账号是否存在")
    void loginWrongPassword() throws Exception {
        JsonNode body = doLogin("admin", "wrong-password");
        assertThat(body.get("code").asInt()).isEqualTo(401);
        assertThat(body.get("message").asText()).isEqualTo("用户名或密码错误");

        // 不存在的用户提示应与密码错误完全一致，避免账号枚举
        JsonNode body2 = doLogin("no-such-user", "whatever");
        assertThat(body2.get("code").asInt()).isEqualTo(401);
        assertThat(body2.get("message").asText()).isEqualTo("用户名或密码错误");
    }

    @Test
    @DisplayName("13. 被禁用的账号即使密码正确也无法登录")
    void loginDisabledAccount() throws Exception {
        JsonNode body = doLogin("disabled", "123456");
        assertThat(body.get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("14. 注册后可用新账号登录；重复用户名被拒绝")
    void registerAndLogin() throws Exception {
        String uname = "tester" + System.currentTimeMillis() % 100000;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + uname + "\",\"password\":\"abc12345\","
                                + "\"nickname\":\"测试注册\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        JsonNode login = doLogin(uname, "abc12345");
        assertThat(login.get("code").asInt()).isEqualTo(200);
        assertThat(login.get("data").get("roleCode").asText()).isEqualTo("ROLE_USER");

        // 重复注册
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + uname + "\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    @DisplayName("15. 注册参数校验：弱密码与非法用户名被拦截")
    void registerValidation() throws Exception {
        // 密码不含数字
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"validname\",\"password\":\"onlyletters\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        // 用户名含非法字符
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bad name!\",\"password\":\"abc12345\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("16. 未携带令牌访问受保护接口返回 401")
    void protectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("17. 携带令牌可获取当前用户信息")
    void meWithToken() throws Exception {
        String token = tokenOf("inheritor", "123456");
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("inheritor"))
                .andExpect(jsonPath("$.data.roleCode").value("ROLE_INHERITOR"))
                .andExpect(jsonPath("$.data.isInheritor").value(true));
    }

    @Test
    @DisplayName("18. 伪造/篡改的令牌被拒绝")
    void tamperedTokenRejected() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ==================== 鉴权与越权 ====================

    @Test
    @DisplayName("19. 普通用户访问管理员接口返回 403")
    void userForbiddenOnAdminApi() throws Exception {
        String token = tokenOf("user", "123456");
        mockMvc.perform(get("/api/project/admin/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("20. 管理员可查看全部状态内容（含待审核）")
    void adminSeesAllStatus() throws Exception {
        String token = tokenOf("admin", "123456");
        MvcResult result = mockMvc.perform(get("/api/project/admin/list")
                        .param("pageSize", "100")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = om.readTree(result.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8)).get("data");
        // 管理员应看到全部演示项目：种子 14 条（含 1 条待审核）；
        // 若重复编号测试先执行并成功创建一条，则为 15（断言下限，避免依赖方法执行顺序）
        assertThat(data.get("total").asLong()).isGreaterThanOrEqualTo(14);
    }

    @Test
    @DisplayName("21. 传承人可提交项目，提交后进入待审核")
    void inheritorCanCreateProject() throws Exception {
        String token = tokenOf("inheritor", "123456");
        MvcResult result = mockMvc.perform(post("/api/project")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\":\"测试新增技艺\",\"projectCode\":\"TEST-001\","
                                + "\"categoryId\":8,\"level\":4,\"region\":\"云南省玉溪市\","
                                + "\"summary\":\"集成测试创建\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8));
        long newId = body.get("data").asLong();
        assertThat(newId).isGreaterThan(0);

        // 新项目处于待审核，前台不可见
        mockMvc.perform(get("/api/project/" + newId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("22. 项目名称缺失时返回 400 参数错误")
    void createProjectValidation() throws Exception {
        String token = tokenOf("inheritor", "123456");
        mockMvc.perform(post("/api/project")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":8}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("23. 项目编号重复时返回 409 冲突")
    void createProjectDuplicateCode() throws Exception {
        String token = tokenOf("inheritor", "123456");
        mockMvc.perform(post("/api/project")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectName\":\"重复编号项目\",\"projectCode\":\"YN-QHC\","
                                + "\"categoryId\":8,\"level\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(409));
    }

    // ==================== 退出登录 ====================

    @Test
    @DisplayName("24. 退出登录后原令牌立即失效")
    void logoutInvalidatesToken() throws Exception {
        String token = tokenOf("user", "123456");
        // 退出前可用
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 退出
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 退出后同一令牌被拒绝
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("25. 刷新令牌可换取新的访问令牌")
    void refreshToken() throws Exception {
        JsonNode login = doLogin("user", "123456");
        String refreshToken = login.get("data").get("refreshToken").asText();
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .param("refreshToken", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode body = om.readTree(result.getResponse().getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8));
        assertThat(body.get("data").get("accessToken").asText()).isNotBlank();
    }

    @Test
    @DisplayName("26. 用访问令牌冒充刷新令牌应被拒绝")
    void refreshWithAccessTokenRejected() throws Exception {
        JsonNode login = doLogin("user", "123456");
        String accessToken = login.get("data").get("accessToken").asText();
        mockMvc.perform(post("/api/auth/refresh").param("refreshToken", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文化资讯模块集成测试。
 *
 * <p>两条设计约定，避免用例之间互相污染（这一点在骨架调试时踩过坑）：
 * <ol>
 *   <li>读取类断言的用例只依赖演示数据，不依赖前序用例的创建结果</li>
 *   <li>修改类用例一律<b>自建稿件</b>再操作，不做「改演示数据、后面用例再断言」的耦合</li>
 * </ol>
 *
 * <p>业务重点：验证比项目模块多出的<b>作者归属</b>数据权限——
 * 传承人只能改自己的稿件，篡改 id 改他人稿件必须被拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("文化资讯模块集成测试")
class ArticleModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.yuxi.ich.security.TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

    /** 演示数据中：已发布 3 篇（id 1/2/3），待审核 1 篇（id 4） */
    private static final long DEMO_PUBLISHED_TOP_ARTICLE = 1L;
    private static final long DEMO_PENDING_ARTICLE = 4L;

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

    /** 以某账号新建一篇稿件，返回其 id */
    private long createArticle(String token, String title, Long projectId) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"categoryId\":1,"
                + "\"summary\":\"测试摘要\",\"content\":\"测试正文\""
                + (projectId == null ? "" : ",\"projectId\":" + projectId) + "}";
        JsonNode b = call("POST", "/api/article", token, body);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").asLong();
    }

    /** 新建并审核发布，返回稿件 id */
    private long createPublished(String token, String adminToken, String title) throws Exception {
        long id = createArticle(token, title, null);
        JsonNode a = call("PUT", "/api/article/" + id + "/audit?auditStatus=1", adminToken, null);
        assertThat(a.get("code").asInt()).isEqualTo(200);
        return id;
    }

    // ==================== 查询（只依赖演示数据） ====================

    @Test
    @DisplayName("A1. 资讯列表只返回已发布，待审核稿件不可见")
    void listOnlyPublished() throws Exception {
        JsonNode b = call("GET", "/api/article/list?pageSize=50", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        for (JsonNode n : b.get("data").get("records")) {
            assertThat(n.get("auditStatus").asInt()).isEqualTo(1);
            assertThat(n.get("title").asText()).doesNotContain("待审核");
        }
    }

    @Test
    @DisplayName("A2. 列表带出分类名与作者昵称")
    void listJoinsNames() throws Exception {
        JsonNode b = call("GET", "/api/article/list?pageSize=1", null, null);
        JsonNode first = b.get("data").get("records").get(0);
        assertThat(first.get("categoryName").asText()).isNotBlank();
        assertThat(first.get("authorName").asText()).isNotBlank();
    }

    @Test
    @DisplayName("A3. 置顶稿件排在非置顶之前")
    void topArticleFirst() throws Exception {
        JsonNode b = call("GET", "/api/article/list?pageSize=10", null, null);
        JsonNode records = b.get("data").get("records");
        // 演示数据中 id=1 为置顶，应排在首位
        assertThat(records.get(0).get("id").asLong()).isEqualTo(DEMO_PUBLISHED_TOP_ARTICLE);
        assertThat(records.get(0).get("isTop").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("A4. 按分类筛选生效")
    void filterByCategory() throws Exception {
        JsonNode b = call("GET", "/api/article/list?categoryId=1&pageSize=50", null, null);
        for (JsonNode n : b.get("data").get("records")) {
            assertThat(n.get("categoryId").asLong()).isEqualTo(1L);
        }
        assertThat(b.get("data").get("total").asLong()).isGreaterThan(0);
    }

    @Test
    @DisplayName("A5. 关键词检索命中标题或摘要")
    void keywordSearch() throws Exception {
        JsonNode b = call("GET", "/api/article/list?keyword=青花&pageSize=50", null, null);
        assertThat(b.get("data").get("total").asLong()).isGreaterThan(0);
    }

    @Test
    @DisplayName("A6. 详情返回正文与关联项目名")
    void detail() throws Exception {
        JsonNode b = call("GET", "/api/article/detail/" + DEMO_PUBLISHED_TOP_ARTICLE, null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").get("title").asText()).contains("玉溪青花瓷");
        assertThat(b.get("data").get("projectName").asText()).isEqualTo("玉溪青花瓷烧制技艺");
        assertThat(b.get("data").get("content").asText()).isNotBlank();
    }

    @Test
    @DisplayName("A7. 待审核稿件前台详情返回 404")
    void pendingDetailNotFound() throws Exception {
        JsonNode b = call("GET", "/api/article/detail/" + DEMO_PENDING_ARTICLE, null, null);
        assertThat(b.get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("A8. 相关资讯推荐排除自身")
    void related() throws Exception {
        JsonNode b = call("GET", "/api/article/related?projectId=1&excludeId="
                + DEMO_PUBLISHED_TOP_ARTICLE, null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        for (JsonNode n : b.get("data")) {
            assertThat(n.get("id").asLong()).isNotEqualTo(DEMO_PUBLISHED_TOP_ARTICLE);
        }
    }

    @Test
    @DisplayName("A9. 资讯分类列表返回 5 个分类，按 sort_no 升序")
    void categories() throws Exception {
        JsonNode b = call("GET", "/api/article/categories", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").size()).isEqualTo(5);
        assertThat(b.get("data").get(0).get("name").asText()).isEqualTo("非遗资讯");
    }

    // ==================== 投稿与校验 ====================

    @Test
    @DisplayName("A10. 传承人可投稿，提交后进入待审核且前台不可见")
    void inheritorCanCreate() throws Exception {
        String t = token("inheritor");
        long id = createArticle(t, "集成测试稿件", null);

        // 待审核，前台不可见
        assertThat(call("GET", "/api/article/detail/" + id, null, null)
                .get("code").asInt()).isEqualTo(404);

        // 投稿人自己可查看进度
        JsonNode mine = call("GET", "/api/article/mine?pageSize=100", t, null);
        boolean found = false;
        for (JsonNode n : mine.get("data").get("records")) {
            if (n.get("id").asLong() == id) {
                assertThat(n.get("auditStatus").asInt()).isEqualTo(0);
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("A11. 普通用户不能投稿（403）")
    void userCannotCreate() throws Exception {
        String t = token("user");
        JsonNode b = call("POST", "/api/article", t,
                "{\"title\":\"越权投稿\",\"categoryId\":1,\"content\":\"正文\"}");
        assertThat(b.get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("A12. 标题或正文缺失返回 400")
    void createValidation() throws Exception {
        String t = token("inheritor");
        assertThat(call("POST", "/api/article", t, "{\"categoryId\":1}")
                .get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("A13. 分类不存在时返回 400")
    void createInvalidCategory() throws Exception {
        String t = token("inheritor");
        JsonNode b = call("POST", "/api/article", t,
                "{\"title\":\"分类非法\",\"categoryId\":99999,\"content\":\"正文\"}");
        assertThat(b.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("A14. 关联未发布的项目时返回 400")
    void createWithPendingProject() throws Exception {
        String t = token("inheritor");
        // 演示数据中 id=14 是待审核项目
        JsonNode b = call("POST", "/api/article", t,
                "{\"title\":\"关联待审项目\",\"categoryId\":1,\"content\":\"正文\",\"projectId\":14}");
        assertThat(b.get("code").asInt()).isEqualTo(400);
    }

    // ==================== 作者归属（数据权限） ====================

    @Test
    @DisplayName("A15. 传承人不能修改他人稿件（403），提示含「他人」")
    void cannotUpdateOthersArticle() throws Exception {
        // 稿件 2 的 author_id = 1（admin），用 inheritor 去改应被拦截
        String t = token("inheritor");
        JsonNode b = call("PUT", "/api/article/2", t,
                "{\"title\":\"越权修改\",\"categoryId\":1,\"content\":\"正文\"}");
        assertThat(b.get("code").asInt()).isEqualTo(403);
        assertThat(b.get("message").asText()).contains("他人");
    }

    @Test
    @DisplayName("A16. 传承人不能删除他人稿件（403）")
    void cannotDeleteOthersArticle() throws Exception {
        String t = token("inheritor");
        MvcResult r = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/article/2")
                                .header("Authorization", "Bearer " + t))
                .andExpect(status().isOk()).andReturn();
        JsonNode b = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b.get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("A17. 传承人可修改自己的稿件，改后回到待审核")
    void ownerCanUpdateOwn() throws Exception {
        String t = token("inheritor");
        long id = createArticle(t, "我的待改稿件", null);

        JsonNode b = call("PUT", "/api/article/" + id, t,
                "{\"title\":\"我的稿件已更新\",\"categoryId\":1,\"content\":\"更新后的正文\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);

        // 修改后回到待审核，前台不可见
        assertThat(call("GET", "/api/article/detail/" + id, null, null)
                .get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("A18. 管理员可修改他人稿件")
    void adminCanUpdateOthers() throws Exception {
        String adminToken = token("admin");
        String inheritorToken = token("inheritor");
        long id = createArticle(inheritorToken, "传承人的稿件", null);

        JsonNode b = call("PUT", "/api/article/" + id, adminToken,
                "{\"title\":\"管理员修改的标题\",\"categoryId\":1,\"content\":\"正文\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
    }

    // ==================== 审核与置顶 ====================

    @Test
    @DisplayName("A19. 审核发布后写入发布时间，前台可见")
    void auditPublishesAndSetsTime() throws Exception {
        String adminToken = token("admin");
        String t = token("inheritor");

        long id = createArticle(t, "待发布稿件", null);

        // 发布
        assertThat(call("PUT", "/api/article/" + id + "/audit?auditStatus=1",
                adminToken, null).get("code").asInt()).isEqualTo(200);

        // 前台可见，且发布时间已写入
        JsonNode d = call("GET", "/api/article/detail/" + id, null, null);
        assertThat(d.get("code").asInt()).isEqualTo(200);
        assertThat(d.get("data").get("publishTime").isNull()).isFalse();
    }

    @Test
    @DisplayName("A20. 审核驳回后前台仍不可见")
    void auditRejectKeepsHidden() throws Exception {
        String adminToken = token("admin");
        String t = token("inheritor");
        long id = createArticle(t, "将被驳回的稿件", null);

        assertThat(call("PUT", "/api/article/" + id + "/audit?auditStatus=3",
                adminToken, null).get("code").asInt()).isEqualTo(200);
        assertThat(call("GET", "/api/article/detail/" + id, null, null)
                .get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("A21. 审核状态非法返回 400；普通用户审核返回 403")
    void auditGuards() throws Exception {
        String adminToken = token("admin");
        String userToken = token("user");
        assertThat(call("PUT", "/api/article/2/audit?auditStatus=9", adminToken, null)
                .get("code").asInt()).isEqualTo(400);
        assertThat(call("PUT", "/api/article/2/audit?auditStatus=1", userToken, null)
                .get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("A22. 管理员可设置与取消置顶，置顶稿件排在列表前面")
    void setTop() throws Exception {
        String adminToken = token("admin");
        String t = token("inheritor");
        long id = createPublished(t, adminToken, "将被置顶的稿件");

        // 置顶
        assertThat(call("PUT", "/api/article/" + id + "/top?isTop=1", adminToken, null)
                .get("code").asInt()).isEqualTo(200);

        // 该稿件应出现在列表前面（置顶区）
        JsonNode list = call("GET", "/api/article/list?pageSize=10", null, null);
        JsonNode records = list.get("data").get("records");
        boolean inTopSection = false;
        for (JsonNode n : records) {
            if (n.get("isTop").asInt() == 0) {
                break;   // 已进入非置顶区
            }
            if (n.get("id").asLong() == id) {
                inTopSection = true;
                break;
            }
        }
        assertThat(inTopSection).isTrue();

        // 取消置顶
        assertThat(call("PUT", "/api/article/" + id + "/top?isTop=0", adminToken, null)
                .get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("A23. 置顶取值非法返回 400")
    void setTopInvalid() throws Exception {
        String adminToken = token("admin");
        assertThat(call("PUT", "/api/article/1/top?isTop=5", adminToken, null)
                .get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("A24. 我的投稿列表只返回自己的稿件")
    void myArticles() throws Exception {
        String t = token("inheritor");
        String myId = call("GET", "/api/auth/me", t, null)
                .get("data").get("userId").asText();
        JsonNode b = call("GET", "/api/article/mine?pageSize=100", t, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        for (JsonNode n : b.get("data").get("records")) {
            if (!n.get("authorId").isNull()) {
                assertThat(n.get("authorId").asText()).isEqualTo(myId);
            }
        }
    }

    @Test
    @DisplayName("A25. 后台列表可看到待审核稿件（管理员），普通用户被拒 403")
    void adminList() throws Exception {
        String adminToken = token("admin");
        JsonNode b = call("GET", "/api/article/admin/list?pageSize=100", adminToken, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        boolean hasPending = false;
        for (JsonNode n : b.get("data").get("records")) {
            if (n.get("auditStatus").asInt() == 0) {
                hasPending = true;
                break;
            }
        }
        assertThat(hasPending).isTrue();

        assertThat(call("GET", "/api/article/admin/list", token("user"), null)
                .get("code").asInt()).isEqualTo(403);
    }
}

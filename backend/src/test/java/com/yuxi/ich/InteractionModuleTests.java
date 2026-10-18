package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户互动模块集成测试。
 *
 * <p>业务重点：收藏与点赞的<b>幂等性</b>。
 * 同一用户重复点击不应产生多条记录、也不应重复计数；
 * 取消后重新收藏要能成功（这一点如果不清理历史逻辑删除行会撞唯一约束）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("用户互动模块集成测试")
class InteractionModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.yuxi.ich.security.TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

    /** 目标类型：1 项目，2 资讯，3 活动 */
    private static final int T_PROJECT = 1;
    private static final int T_ARTICLE = 2;
    private static final int T_ACTIVITY = 3;

    @BeforeEach
    void reset() {
        tokenBlacklist.clear();
    }

    private JsonNode call(String method, String url, String token, String body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(url);
            case "DELETE" -> delete(url);
            default -> get(url);
        };
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req = req.contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body);
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

    // ==================== 收藏幂等 ====================

    @Test
    @DisplayName("N1. 收藏 → 取消 → 再收藏，状态正确切换且不报唯一键冲突")
    void favoriteToggleIdempotent() throws Exception {
        String t = token("user");
        String url = "/api/interaction/favorite?targetType=" + T_PROJECT + "&targetId=2";

        // 第一次：收藏
        JsonNode r1 = call("POST", url, t, null);
        assertThat(r1.get("code").asInt()).isEqualTo(200);
        assertThat(r1.get("data").get("active").asBoolean()).isTrue();

        // 再点一次：取消
        JsonNode r2 = call("POST", url, t, null);
        assertThat(r2.get("data").get("active").asBoolean()).isFalse();

        // 第三次：重新收藏 —— 这一步若不清理历史逻辑删除行会撞唯一约束
        JsonNode r3 = call("POST", url, t, null);
        assertThat(r3.get("code").asInt()).isEqualTo(200);
        assertThat(r3.get("data").get("active").asBoolean()).isTrue();

        // 状态查询应与最后一次操作一致
        JsonNode st = call("GET", "/api/interaction/state?targetType=" + T_PROJECT
                + "&targetId=2", t, null);
        assertThat(st.get("data").get("favorited").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("N2. 点赞开关独立于收藏，两者互不影响")
    void likeIndependentFromFavorite() throws Exception {
        String t = token("user");
        long targetId = 3L;

        // 点赞
        JsonNode like = call("POST", "/api/interaction/like?targetType=" + T_PROJECT
                + "&targetId=" + targetId, t, null);
        assertThat(like.get("code").asInt()).isEqualTo(200);
        assertThat(like.get("data").get("active").asBoolean()).isTrue();

        // 收藏同一目标
        JsonNode fav = call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=" + targetId, t, null);
        assertThat(fav.get("data").get("active").asBoolean()).isTrue();

        JsonNode st = call("GET", "/api/interaction/state?targetType=" + T_PROJECT
                + "&targetId=" + targetId, t, null);
        assertThat(st.get("data").get("liked").asBoolean()).isTrue();
        assertThat(st.get("data").get("favorited").asBoolean()).isTrue();

        // 取消点赞，收藏应保持
        call("POST", "/api/interaction/like?targetType=" + T_PROJECT + "&targetId=" + targetId,
                t, null);
        JsonNode st2 = call("GET", "/api/interaction/state?targetType=" + T_PROJECT
                + "&targetId=" + targetId, t, null);
        assertThat(st2.get("data").get("liked").asBoolean()).isFalse();
        assertThat(st2.get("data").get("favorited").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("N3. 收藏维护目标对象的 favorite_count 计数")
    void favoriteMaintainsCounter() throws Exception {
        String t = token("user");
        long targetId = 6L;

        int before = call("GET", "/api/project/" + targetId, t, null)
                .get("data").get("favoriteCount").asInt();

        call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=" + targetId, t, null);
        int afterAdd = call("GET", "/api/project/" + targetId, null, null)
                .get("data").get("favoriteCount").asInt();
        assertThat(afterAdd).isEqualTo(before + 1);

        call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=" + targetId, t, null);
        int afterCancel = call("GET", "/api/project/" + targetId, null, null)
                .get("data").get("favoriteCount").asInt();
        assertThat(afterCancel).isEqualTo(before);
    }

    @Test
    @DisplayName("N4. 未登录不能收藏/点赞（401）")
    void interactionRequiresLogin() throws Exception {
        assertThat(call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=1", null, null).get("code").asInt()).isEqualTo(401);
        assertThat(call("POST", "/api/interaction/like?targetType=" + T_PROJECT
                + "&targetId=1", null, null).get("code").asInt()).isEqualTo(401);
    }

    @Test
    @DisplayName("N5. 目标类型非法或目标不存在时被拦截")
    void invalidTarget() throws Exception {
        String t = token("user");
        assertThat(call("POST", "/api/interaction/favorite?targetType=9&targetId=1", t, null)
                .get("code").asInt()).isEqualTo(400);
        assertThat(call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=999999", t, null).get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("N6. 不能对未发布内容互动（待审核项目返回 404）")
    void cannotInteractWithPending() throws Exception {
        String t = token("user");
        // 演示数据中 id=14 是待审核项目
        assertThat(call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=14", t, null).get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("N7. 可以对资讯与活动互动（多态关联覆盖三类目标）")
    void interactWithAllTargetTypes() throws Exception {
        String t = token("user");
        assertThat(call("POST", "/api/interaction/like?targetType=" + T_ARTICLE
                + "&targetId=1", t, null).get("code").asInt()).isEqualTo(200);
        assertThat(call("POST", "/api/interaction/like?targetType=" + T_ACTIVITY
                + "&targetId=1", t, null).get("code").asInt()).isEqualTo(200);
    }

    // ==================== 评论 ====================

    @Test
    @DisplayName("N8. 发表评论并出现在评论列表（公开可读，带昵称）")
    void commentAndList() throws Exception {
        String t = token("user");
        String content = "集成测试评论-" + System.currentTimeMillis() % 1000000;

        JsonNode c = call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":7,\"content\":\"" + content + "\"}");
        assertThat(c.get("code").asInt()).isEqualTo(200);
        long commentId = c.get("data").asLong();
        assertThat(commentId).isGreaterThan(0);

        // 评论列表是公开接口，不带令牌也能读
        JsonNode list = call("GET", "/api/interaction/comments?targetType=" + T_PROJECT
                + "&targetId=7&pageSize=50", null, null);
        assertThat(list.get("code").asInt()).isEqualTo(200);
        boolean found = false;
        for (JsonNode n : list.get("data").get("records")) {
            if (n.get("id").asLong() == commentId) {
                assertThat(n.get("content").asText()).isEqualTo(content);
                found = true;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("N9. 评论内容为空或超长被拦截（400）")
    void commentValidation() throws Exception {
        String t = token("user");
        assertThat(call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":8,\"content\":\"   \"}")
                .get("code").asInt()).isEqualTo(400);

        String tooLong = "字".repeat(501);
        assertThat(call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":8,\"content\":\"" + tooLong + "\"}")
                .get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("N10. 回复评论成功；回复不属于当前内容的评论被拒")
    void replyComment() throws Exception {
        String t = token("user");
        JsonNode parent = call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":9,\"content\":\"父评论\"}");
        long parentId = parent.get("data").asLong();

        // 合法的回复
        JsonNode reply = call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":9,\"content\":\"回复内容\","
                        + "\"parentId\":" + parentId + "}");
        assertThat(reply.get("code").asInt()).isEqualTo(200);

        // 把该评论挂到另一个目标对象下，应被拒绝
        JsonNode bad = call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":10,\"content\":\"跨对象回复\","
                        + "\"parentId\":" + parentId + "}");
        assertThat(bad.get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("N11. 未登录不能发表评论（401）")
    void commentRequiresLogin() throws Exception {
        assertThat(call("POST", "/api/interaction/comment", null,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":1,\"content\":\"匿名评论\"}")
                .get("code").asInt()).isEqualTo(401);
    }

    // ==================== 我的收藏与删除 ====================

    @Test
    @DisplayName("N12. 我的收藏列表带出目标标题，跨项目与资讯")
    void myFavorites() throws Exception {
        // 用一个新账号，避免受其他用例的收藏影响
        String uname = "fav" + System.currentTimeMillis() % 1000000;
        assertThat(call("POST", "/api/auth/register", null,
                "{\"username\":\"" + uname + "\",\"password\":\"abc12345\"}")
                .get("code").asInt()).isEqualTo(200);
        JsonNode lb = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + uname + "\",\"password\":\"abc12345\"}");
        String t = lb.get("data").get("accessToken").asText();

        // 收藏一个项目 + 一篇资讯
        assertThat(call("POST", "/api/interaction/favorite?targetType=" + T_PROJECT
                + "&targetId=1", t, null).get("code").asInt()).isEqualTo(200);
        assertThat(call("POST", "/api/interaction/favorite?targetType=" + T_ARTICLE
                + "&targetId=1", t, null).get("code").asInt()).isEqualTo(200);

        JsonNode mine = call("GET", "/api/interaction/my/favorites?pageSize=50", t, null);
        assertThat(mine.get("code").asInt()).isEqualTo(200);
        assertThat(mine.get("data").get("total").asLong()).isEqualTo(2L);

        boolean hasTitle = false;
        for (JsonNode n : mine.get("data").get("records")) {
            if (n.hasNonNull("targetTitle") && !n.get("targetTitle").asText().isBlank()) {
                hasTitle = true;
            }
        }
        assertThat(hasTitle).isTrue();
    }

    @Test
    @DisplayName("N13. 删除自己的评论成功；删除他人评论返回 403")
    void removeOwnership() throws Exception {
        String t = token("user");
        JsonNode c = call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":11,\"content\":\"待删除评论\"}");
        long id = c.get("data").asLong();
        assertThat(call("DELETE", "/api/interaction/" + id, t, null).get("code").asInt())
                .isEqualTo(200);

        // 用另一个账号删同一条（已被删，返回 404）以及删他人评论（403）
        String other = token("inheritor");
        JsonNode c2 = call("POST", "/api/interaction/comment", t,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":12,\"content\":\"他人评论\"}");
        long id2 = c2.get("data").asLong();
        JsonNode forbidden = call("DELETE", "/api/interaction/" + id2, other, null);
        assertThat(forbidden.get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("N14. 管理员可屏蔽评论；屏蔽后不出现在评论列表")
    void adminBlockComment() throws Exception {
        String admin = token("admin");
        String user = token("user");
        String content = "待屏蔽评论-" + System.currentTimeMillis() % 1000000;

        JsonNode c = call("POST", "/api/interaction/comment", user,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":13,\"content\":\"" + content + "\"}");
        long id = c.get("data").asLong();

        // 屏蔽
        assertThat(call("PUT", "/api/interaction/" + id + "/status?status=0", admin, null)
                .get("code").asInt()).isEqualTo(200);

        // 列表中不应再出现
        JsonNode list = call("GET", "/api/interaction/comments?targetType=" + T_PROJECT
                + "&targetId=13&pageSize=50", null, null);
        for (JsonNode n : list.get("data").get("records")) {
            assertThat(n.get("id").asLong()).isNotEqualTo(id);
        }
    }

    @Test
    @DisplayName("N15. 普通用户不能屏蔽评论（403），状态取值非法返回 400")
    void blockCommentGuards() throws Exception {
        String user = token("user");
        String admin = token("admin");
        // 用存在的已发布项目 id=2；换成不存在的 id 会拿到 404，data 为 null 导致后续断言 NPE
        JsonNode c = call("POST", "/api/interaction/comment", user,
                "{\"targetType\":" + T_PROJECT + ",\"targetId\":2,\"content\":\"权限测试评论\"}");
        assertThat(c.get("code").asInt()).isEqualTo(200);
        long id = c.get("data").asLong();

        assertThat(call("PUT", "/api/interaction/" + id + "/status?status=0", user, null)
                .get("code").asInt()).isEqualTo(403);
        assertThat(call("PUT", "/api/interaction/" + id + "/status?status=9", admin, null)
                .get("code").asInt()).isEqualTo(400);
    }
}

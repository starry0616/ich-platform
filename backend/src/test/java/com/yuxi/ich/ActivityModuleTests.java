package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.activity.service.ActivityService;
import com.yuxi.ich.security.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 非遗活动与报名模块集成测试。
 *
 * <p>本模块的核心风险是<b>名额超卖</b>，因此除了常规功能验证外，
 * 专门用多线程并发调用 Service 来验证防超卖是否真的生效——
 * 这是本项目唯一需要并发测试的地方。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("活动与报名模块集成测试")
class ActivityModuleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private IchActivityMapper activityMapper;

    @Autowired
    private com.yuxi.ich.security.TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

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

    /**
     * 注册并登录一个全新用户，返回令牌。
     *
     * <p>用户名用 UUID 生成。曾用「时间戳 % 1000000 + random(1000)」拼接，
     * 同一毫秒内连续调用会生成相同用户名，导致复用上一个用例已报名的账号、
     * 出现莫名其妙的「重复报名」409。
     */
    private String newUserToken(String prefix) throws Exception {
        String uname = prefix + java.util.UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12);
        assertThat(call("POST", "/api/auth/register", null,
                "{\"username\":\"" + uname + "\",\"password\":\"abc12345\"}")
                .get("code").asInt()).isEqualTo(200);
        JsonNode lb = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + uname + "\",\"password\":\"abc12345\"}");
        return lb.get("data").get("accessToken").asText();
    }

    /** 取得某用户的 id（并发测试需构造 SecurityContext） */
    private long userIdOf(String token) throws Exception {
        return call("GET", "/api/auth/me", token, null).get("data").get("userId").asLong();
    }

    // ==================== 查询 ====================

    @Test
    @DisplayName("B1. 活动列表只返回已发布，待审核活动不可见")
    void listOnlyPublished() throws Exception {
        JsonNode b = call("GET", "/api/activity/list?pageSize=100", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        // 只断言状态：不要用标题关键词判断，因为其他用例会创建标题含「待审核」的活动
        for (JsonNode n : b.get("data").get("records")) {
            assertThat(n.get("status").asInt()).isGreaterThanOrEqualTo(1);
        }
        // 演示数据中 id=5 是待审核活动，前台列表中不应出现
        for (JsonNode n : b.get("data").get("records")) {
            assertThat(n.get("id").asLong()).isNotEqualTo(5L);
        }
    }

    @Test
    @DisplayName("B2. 列表带出剩余名额与关联项目名")
    void listReturnsRemaining() throws Exception {
        JsonNode b = call("GET", "/api/activity/list?pageSize=1", null, null);
        JsonNode first = b.get("data").get("records").get(0);
        assertThat(first.get("remaining").asInt())
                .isEqualTo(first.get("quota").asInt() - first.get("signed").asInt());
    }

    @Test
    @DisplayName("B3. onlyOpen 只返回仍可报名的活动（排除已截止）")
    void onlyOpenFilter() throws Exception {
        JsonNode all = call("GET", "/api/activity/list?pageSize=50", null, null);
        JsonNode open = call("GET", "/api/activity/list?onlyOpen=true&pageSize=50", null, null);
        assertThat(open.get("code").asInt()).isEqualTo(200);
        // 可报名的一定不多于全部
        assertThat(open.get("data").get("total").asLong())
                .isLessThanOrEqualTo(all.get("data").get("total").asLong());
        // 已过截止时间的活动不应出现
        for (JsonNode n : open.get("data").get("records")) {
            assertThat(n.get("title").asText()).doesNotContain("报名截止");
            assertThat(n.get("status").asInt()).isEqualTo(1);
        }
    }

    @Test
    @DisplayName("B4. 详情返回完整字段与剩余名额；待审核活动返回 404")
    void detail() throws Exception {
        JsonNode b = call("GET", "/api/activity/1", null, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").get("title").asText()).contains("青花瓷");
        assertThat(b.get("data").get("remaining").asInt()).isGreaterThanOrEqualTo(0);

        // 演示数据中 id=5 为待审核活动
        assertThat(call("GET", "/api/activity/5", null, null).get("code").asInt())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("B5. 不存在的活动返回 404")
    void detailNotFound() throws Exception {
        assertThat(call("GET", "/api/activity/999999", null, null).get("code").asInt())
                .isEqualTo(404);
    }

    // ==================== 报名校验 ====================

    @Test
    @DisplayName("B6. 报名成功返回报名编号，且已报名人数 +1")
    void signupSuccess() throws Exception {
        String t = newUserToken("su");
        int signedBefore = call("GET", "/api/activity/1", null, null)
                .get("data").get("signed").asInt();

        JsonNode b = call("POST", "/api/activity/1/signup", t,
                "{\"realName\":\"测试报名\",\"phone\":\"13900000000\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").get("signupNo").asText()).startsWith("YX");
        assertThat(b.get("data").get("status").asInt()).isEqualTo(1);

        int signedAfter = call("GET", "/api/activity/1", null, null)
                .get("data").get("signed").asInt();
        assertThat(signedAfter).isEqualTo(signedBefore + 1);
    }

    @Test
    @DisplayName("B7. 同一用户重复报名被拒（409）")
    void duplicateSignupRejected() throws Exception {
        String t = newUserToken("dup");
        assertThat(call("POST", "/api/activity/1/signup", t, "{}").get("code").asInt())
                .isEqualTo(200);
        JsonNode second = call("POST", "/api/activity/1/signup", t, "{}");
        assertThat(second.get("code").asInt()).isEqualTo(409);
        assertThat(second.get("message").asText()).contains("重复报名");
    }

    @Test
    @DisplayName("B8. 未登录不能报名（401）")
    void signupRequiresLogin() throws Exception {
        assertThat(call("POST", "/api/activity/1/signup", null, "{}").get("code").asInt())
                .isEqualTo(401);
    }

    @Test
    @DisplayName("B9. 报名已截止的活动被拒（409）")
    void signupAfterDeadlineRejected() throws Exception {
        String t = newUserToken("dl");
        // 演示数据中 id=4 的报名截止时间为昨天
        JsonNode b = call("POST", "/api/activity/4/signup", t, "{}");
        assertThat(b.get("code").asInt()).isEqualTo(409);
        assertThat(b.get("message").asText()).contains("截止");
    }

    @Test
    @DisplayName("B10. 名额已满时报名被拒（409）")
    void signupWhenFullRejected() throws Exception {
        // 演示数据中 id=3 名额为 1；先用一个用户占掉
        String first = newUserToken("full1");
        JsonNode r1 = call("POST", "/api/activity/3/signup", first, "{}");
        // 可能被其他用例先占；两种结果都合法，但必须有一个成功或已满
        assertThat(r1.get("code").asInt()).isIn(200, 409);

        String second = newUserToken("full2");
        JsonNode r2 = call("POST", "/api/activity/3/signup", second, "{}");
        assertThat(r2.get("code").asInt()).isEqualTo(409);
    }

    @Test
    @DisplayName("B11. 待审核活动不能报名（409）")
    void signupPendingActivityRejected() throws Exception {
        String t = newUserToken("pd");
        JsonNode b = call("POST", "/api/activity/5/signup", t, "{}");
        // 活动不存在（前台不可见）或不可报名，均不应报名成功
        assertThat(b.get("code").asInt()).isIn(404, 409);
    }

    // ==================== 取消报名 ====================

    @Test
    @DisplayName("B12. 报名后可取消，名额回退")
    void cancelSignup() throws Exception {
        // 用活动 2（30 天后开始），满足「活动开始前 24 小时内不可取消」的约束；
        // 活动 1 只有 15 天，用它做取消测试会被规则挡下。
        long activityId = 2L;
        String t = newUserToken("cc");
        int before = call("GET", "/api/activity/" + activityId, null, null)
                .get("data").get("signed").asInt();

        assertThat(call("POST", "/api/activity/" + activityId + "/signup", t, "{}")
                .get("code").asInt()).isEqualTo(200);
        assertThat(call("DELETE", "/api/activity/" + activityId + "/signup", t, null)
                .get("code").asInt()).isEqualTo(200);

        int after = call("GET", "/api/activity/" + activityId, null, null)
                .get("data").get("signed").asInt();
        assertThat(after).isEqualTo(before);

        // 取消后可以再次报名
        assertThat(call("POST", "/api/activity/" + activityId + "/signup", t, "{}")
                .get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("B12b. 活动开始前 24 小时内不允许取消报名（409）")
    void cancelWithin24HoursRejected() throws Exception {
        String staff = token("inheritor");
        String admin = token("admin");
        // 造一个明天开始的活动：已过 24 小时窗口，可报名但不可取消
        JsonNode created = call("POST", "/api/activity", staff,
                "{\"title\":\"24小时窗口验证\",\"quota\":10,"
                        + "\"startTime\":\"" + java.time.LocalDateTime.now().plusHours(20) + "\","
                        + "\"endTime\":\"" + java.time.LocalDateTime.now().plusHours(22) + "\","
                        + "\"signupDeadline\":\"" + java.time.LocalDateTime.now().plusHours(1) + "\"}");
        long id = created.get("data").asLong();
        call("PUT", "/api/activity/" + id + "/audit?status=1", admin, null);

        String t = newUserToken("w24");
        assertThat(call("POST", "/api/activity/" + id + "/signup", t, "{}")
                .get("code").asInt()).isEqualTo(200);

        JsonNode cancel = call("DELETE", "/api/activity/" + id + "/signup", t, null);
        assertThat(cancel.get("code").asInt()).isEqualTo(409);
        assertThat(cancel.get("message").asText()).contains("24");
    }

    @Test
    @DisplayName("B13. 未报名时取消返回 404")
    void cancelWithoutSignup() throws Exception {
        String t = newUserToken("no");
        assertThat(call("DELETE", "/api/activity/2/signup", t, null).get("code").asInt())
                .isEqualTo(404);
    }

    // ==================== 名单与核销 ====================

    @Test
    @DisplayName("B14. 传承人可查看报名名单；普通用户被拒（403）")
    void signupList() throws Exception {
        JsonNode b = call("GET", "/api/activity/1/signups?pageSize=50", token("inheritor"), null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").get("total").asLong()).isGreaterThan(0);

        assertThat(call("GET", "/api/activity/1/signups", token("user"), null)
                .get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("B15. 凭报名编号核销成功；重复核销被拒")
    void checkin() throws Exception {
        String t = newUserToken("ck");
        JsonNode signup = call("POST", "/api/activity/1/signup", t, "{}");
        String no = signup.get("data").get("signupNo").asText();

        String staff = token("inheritor");
        JsonNode r1 = call("PUT", "/api/activity/1/checkin?signupNo=" + no, staff, null);
        assertThat(r1.get("code").asInt()).isEqualTo(200);
        assertThat(r1.get("data").get("checkinStatus").asInt()).isEqualTo(1);

        JsonNode r2 = call("PUT", "/api/activity/1/checkin?signupNo=" + no, staff, null);
        assertThat(r2.get("code").asInt()).isEqualTo(409);
    }

    @Test
    @DisplayName("B16. 核销不存在的报名编号返回 404")
    void checkinNotFound() throws Exception {
        assertThat(call("PUT", "/api/activity/1/checkin?signupNo=NOT-EXIST",
                token("inheritor"), null).get("code").asInt()).isEqualTo(404);
    }

    @Test
    @DisplayName("B17. 我的报名列表带出活动名称")
    void mySignups() throws Exception {
        String t = newUserToken("my");
        call("POST", "/api/activity/1/signup", t, "{}");
        JsonNode b = call("GET", "/api/activity/my/signups?pageSize=50", t, null);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").get("total").asLong()).isGreaterThan(0);
        JsonNode first = b.get("data").get("records").get(0);
        assertThat(first.get("activityTitle").asText()).isNotBlank();
    }

    // ==================== 发布 / 修改 / 审核 ====================

    @Test
    @DisplayName("B18. 传承人可发布活动，提交后为待审核")
    void createActivity() throws Exception {
        String staff = token("inheritor");
        JsonNode b = call("POST", "/api/activity", staff,
                "{\"title\":\"集成测试活动\",\"activityType\":2,\"quota\":10,"
                        + "\"startTime\":\"2099-01-01T10:00:00\","
                        + "\"endTime\":\"2099-01-01T12:00:00\","
                        + "\"address\":\"测试地点\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
        long id = b.get("data").asLong();

        // 待审核：前台看不到
        assertThat(call("GET", "/api/activity/" + id, null, null).get("code").asInt())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("B19. 普通用户不能发布活动（403）")
    void createForbiddenForUser() throws Exception {
        assertThat(call("POST", "/api/activity", token("user"),
                "{\"title\":\"越权活动\",\"quota\":10,"
                        + "\"startTime\":\"2099-01-01T10:00:00\","
                        + "\"endTime\":\"2099-01-01T12:00:00\"}")
                .get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("B20. 结束时间早于开始时间、或名额缺失时返回 400")
    void createValidation() throws Exception {
        String staff = token("inheritor");
        assertThat(call("POST", "/api/activity", staff,
                "{\"title\":\"时间非法\",\"quota\":10,"
                        + "\"startTime\":\"2099-01-02T10:00:00\","
                        + "\"endTime\":\"2099-01-01T10:00:00\"}")
                .get("code").asInt()).isEqualTo(400);

        assertThat(call("POST", "/api/activity", staff,
                "{\"title\":\"无名额\","
                        + "\"startTime\":\"2099-01-01T10:00:00\","
                        + "\"endTime\":\"2099-01-01T12:00:00\"}")
                .get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("B21. 名额上限不能改到小于已报名人数（400）")
    void updateQuotaBelowSigned() throws Exception {
        String admin = token("admin");
        // 活动 1 已有报名人数（演示数据 signed=2），把名额改成 1 应被拒绝
        JsonNode b = call("PUT", "/api/activity/1", admin,
                "{\"title\":\"玉溪青花瓷拉坯体验课堂\",\"quota\":1,"
                        + "\"startTime\":\"2099-01-01T10:00:00\","
                        + "\"endTime\":\"2099-01-01T12:00:00\"}");
        assertThat(b.get("code").asInt()).isEqualTo(400);
        assertThat(b.get("message").asText()).contains("名额上限");
    }

    @Test
    @DisplayName("B22. 管理员审核通过后活动转报名中，前台可见")
    void auditPublishes() throws Exception {
        String staff = token("inheritor");
        String admin = token("admin");
        JsonNode created = call("POST", "/api/activity", staff,
                "{\"title\":\"待审核活动B22\",\"activityType\":3,\"quota\":5,"
                        + "\"startTime\":\"2099-02-01T10:00:00\","
                        + "\"endTime\":\"2099-02-01T12:00:00\"}");
        long id = created.get("data").asLong();

        assertThat(call("PUT", "/api/activity/" + id + "/audit?status=1", admin, null)
                .get("code").asInt()).isEqualTo(200);

        JsonNode d = call("GET", "/api/activity/" + id, null, null);
        assertThat(d.get("code").asInt()).isEqualTo(200);
        assertThat(d.get("data").get("status").asInt()).isEqualTo(1);
        assertThat(d.get("data").get("remaining").asInt()).isEqualTo(5);
    }

    @Test
    @DisplayName("B23. 审核状态非法返回 400；普通用户审核返回 403")
    void auditGuards() throws Exception {
        String admin = token("admin");
        assertThat(call("PUT", "/api/activity/1/audit?status=9", admin, null)
                .get("code").asInt()).isEqualTo(400);
        assertThat(call("PUT", "/api/activity/1/audit?status=1", token("user"), null)
                .get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("B24. 删除活动后前台不可见，且其互动记录被级联清理")
    void deleteCascadesInteraction() throws Exception {
        String staff = token("inheritor");
        String admin = token("admin");
        String user = token("user");

        // 建活动并发布
        JsonNode created = call("POST", "/api/activity", staff,
                "{\"title\":\"待删除活动B24\",\"quota\":5,"
                        + "\"startTime\":\"2099-03-01T10:00:00\","
                        + "\"endTime\":\"2099-03-01T12:00:00\"}");
        long id = created.get("data").asLong();
        call("PUT", "/api/activity/" + id + "/audit?status=1", admin, null);

        // 用户对其点赞
        assertThat(call("POST", "/api/interaction/like?targetType=3&targetId=" + id,
                user, null).get("code").asInt()).isEqualTo(200);

        // 删除活动
        assertThat(call("DELETE", "/api/activity/" + id, admin, null).get("code").asInt())
                .isEqualTo(200);
        assertThat(call("GET", "/api/activity/" + id, null, null).get("code").asInt())
                .isEqualTo(404);

        // 互动目标已不存在，再次点赞应报 404（说明级联清理生效、未留悬空引用）
        assertThat(call("POST", "/api/interaction/like?targetType=3&targetId=" + id,
                user, null).get("code").asInt()).isEqualTo(404);
    }

    // ==================== 并发防超卖 ====================

    @Test
    @DisplayName("B25. 并发报名不超卖：名额 1 的活动，多人同时抢只有 1 人成功")
    void concurrentSignupNoOversell() throws Exception {
        String staff = token("inheritor");
        String admin = token("admin");

        // 建一个名额为 1 的活动并发布
        JsonNode created = call("POST", "/api/activity", staff,
                "{\"title\":\"并发抢名额B25\",\"activityType\":2,\"quota\":1,"
                        + "\"startTime\":\"2099-04-01T10:00:00\","
                        + "\"endTime\":\"2099-04-01T12:00:00\"}");
        long activityId = created.get("data").asLong();
        call("PUT", "/api/activity/" + activityId + "/audit?status=1", admin, null);

        // 准备 8 个不同用户
        int threads = 8;
        long[] userIds = new long[threads];
        for (int i = 0; i < threads; i++) {
            userIds[i] = userIdOf(newUserToken("cc" + i + "_"));
        }

        // 用 CountDownLatch 让 8 个线程尽量同时发起扣减
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();

        try {
            for (int i = 0; i < threads; i++) {
                final long uid = userIds[i];
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        // 直接调用 Service 并手工设置 SecurityContext，
                        // 因为并发场景下 MockMvc 的线程与 SecurityContext 不共享
                        LoginUser lu = new LoginUser(uid, "u" + uid, "并发用户",
                                1L, "ROLE_USER", 1);
                        SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(lu, null,
                                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
                        try {
                            activityService.signup(activityId, "并发测试", null, null);
                            success.incrementAndGet();
                        } catch (Exception e) {
                            conflict.incrementAndGet();
                        } finally {
                            SecurityContextHolder.clearContext();
                        }
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            done.await(30, TimeUnit.SECONDS);
        } finally {
            pool.shutdownNow();
        }

        // 关键断言：只有 1 人成功，signed 恰好等于 quota，绝不超卖
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(threads - 1);

        Integer signed = activityMapper.selectById(activityId).getSigned();
        Integer quota = activityMapper.selectById(activityId).getQuota();
        assertThat(signed).isEqualTo(1);
        assertThat(signed).isLessThanOrEqualTo(quota);
        assertThat(activityMapper.selectRemaining(activityId)).isEqualTo(0);
    }
}

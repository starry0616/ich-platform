package com.yuxi.ich;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.activity.entity.IchActivity;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.article.entity.CmsArticle;
import com.yuxi.ich.article.mapper.CmsArticleMapper;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.inheritor.dto.InheritorApplyRequest;
import com.yuxi.ich.inheritor.entity.IchInheritor;
import com.yuxi.ich.inheritor.mapper.IchInheritorMapper;
import com.yuxi.ich.inheritor.service.InheritorService;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.mapper.SysUserMapper;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 收藏链路 / 统计口径 / 驳回重提证件号一致性测试（第 13—16 批）。
 *
 * <p>全部使用 H2 内存库，临时账号与临时传承人用后即删，不连接 MySQL。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("收藏链路与统计口径一致性测试")
class FlowConsistencyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private IchInheritorMapper inheritorMapper;

    @Autowired
    private InheritorService inheritorService;

    @Autowired
    private CmsArticleMapper articleMapper;

    @Autowired
    private IchActivityMapper activityMapper;

    private final ObjectMapper om = new ObjectMapper();
    private final List<Long> tempUserIds = new ArrayList<>();
    private final List<Long> tempInheritorIds = new ArrayList<>();
    private final List<Long> tempArticleIds = new ArrayList<>();
    private final List<Long> tempActivityIds = new ArrayList<>();

    @BeforeEach
    void reset() {
    }

    @AfterEach
    void cleanup() {
        for (Long id : tempActivityIds) {
            activityMapper.deleteById(id);
        }
        tempActivityIds.clear();
        for (Long id : tempArticleIds) {
            articleMapper.deleteById(id);
        }
        tempArticleIds.clear();
        for (Long id : tempInheritorIds) {
            inheritorMapper.deleteById(id);
        }
        tempInheritorIds.clear();
        for (Long id : tempUserIds) {
            userMapper.deleteById(id);
        }
        tempUserIds.clear();
    }

    // ==================== B1. 收藏双链路一致 + 用户隔离 ====================

    @Test
    @DisplayName("B1a. 收藏项目后 /interaction/my/favorites 与 /user/favorites 同时可见；取消后同时消失")
    void favoriteVisibleOnBothEndpoints() throws Exception {
        String token = login("user");

        // 收藏项目 1
        postFavorite(token, 1L, true);
        assertThat(hasFavorite(token, "/api/interaction/my/favorites", 1L)).isTrue();
        assertThat(hasFavorite(token, "/api/user/favorites", 1L)).isTrue();

        // 取消收藏
        postFavorite(token, 1L, false);
        assertThat(hasFavorite(token, "/api/interaction/my/favorites", 1L)).isFalse();
        assertThat(hasFavorite(token, "/api/user/favorites", 1L)).isFalse();
    }

    @Test
    @DisplayName("B1b. 用户之间收藏隔离：另一账号看不到 user 的收藏")
    void favoritesAreIsolatedPerUser() throws Exception {
        String userToken = login("user");
        postFavorite(userToken, 1L, true);

        String otherName = "fav_other_" + System.nanoTime();
        registerUser(otherName);
        String otherToken = loginByName(otherName, "Abc12345");

        assertThat(hasFavorite(otherToken, "/api/interaction/my/favorites", 1L)).isFalse();
        assertThat(hasFavorite(otherToken, "/api/user/favorites", 1L)).isFalse();

        // 恢复 user 收藏状态，避免污染
        postFavorite(userToken, 1L, false);
    }

    @Test
    @DisplayName("B1c. 收藏已认证传承人后，新旧两个收藏接口同时可见；取消后同时消失")
    void inheritorFavoriteVisibleOnBothEndpoints() throws Exception {
        IchInheritor certified = inheritorMapper.selectOne(
                Wrappers.<IchInheritor>lambdaQuery()
                        .eq(IchInheritor::getCertStatus, StatusConstants.CertStatus.CERTIFIED)
                        .last("LIMIT 1"));
        if (certified == null) {
            return; // 演示库无已认证传承人时跳过，不断言
        }
        String token = login("user");

        postFavoriteType(token, 4, certified.getId(), true);
        assertThat(hasFavorite(token, "/api/interaction/my/favorites", certified.getId())).isTrue();
        assertThat(hasFavorite(token, "/api/user/favorites", certified.getId())).isTrue();

        postFavoriteType(token, 4, certified.getId(), false);
        assertThat(hasFavorite(token, "/api/interaction/my/favorites", certified.getId())).isFalse();
        assertThat(hasFavorite(token, "/api/user/favorites", certified.getId())).isFalse();
    }

    // ==================== B3. 公开/后台统计口径分离 ====================

    @Test
    @DisplayName("B3a. 公开 overview 只计已发布项目/已认证传承人；后台 overview 计全量")
    void publicAndAdminStatsDiffer() throws Exception {
        JsonNode pub = getJson(get("/api/stats/overview"));
        assertThat(pub.get("code").asInt()).isEqualTo(200);
        int pubProjects = pub.get("data").get("projects").asInt();
        int pubInheritors = pub.get("data").get("inheritors").asInt();

        String adminToken = login("admin");
        JsonNode admin = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken));
        assertThat(admin.get("code").asInt()).isEqualTo(200);
        int adminProjects = admin.get("data").get("projects").asInt();
        int adminInheritors = admin.get("data").get("inheritors").asInt();

        // 后台项目数 >= 公开（含待审核），传承人同理
        assertThat(adminProjects).isGreaterThanOrEqualTo(pubProjects);
        assertThat(adminInheritors).isGreaterThanOrEqualTo(pubInheritors);
    }

    @Test
    @DisplayName("B3b. 非已认证传承人不计入公开统计，但计入后台全量")
    void pendingInheritorExcludedFromPublic() throws Exception {
        String adminToken = login("admin");
        // 先取基线
        int pubBase = getJson(get("/api/stats/overview"))
                .get("data").get("inheritors").asInt();
        int adminBase = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken))
                .get("data").get("inheritors").asInt();

        // 造一条待审核传承人（cert_status=0）
        IchInheritor pending = new IchInheritor();
        pending.setName("待审核统计测试");
        pending.setLevel(2);
        pending.setRegion("测试地区");
        pending.setCertNo("STAT-PENDING-" + System.nanoTime());
        pending.setCertStatus(StatusConstants.CertStatus.PENDING);
        pending.setViewCount(0);
        inheritorMapper.insert(pending);
        tempInheritorIds.add(pending.getId());

        int pubAfter = getJson(get("/api/stats/overview"))
                .get("data").get("inheritors").asInt();
        int adminAfter = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken))
                .get("data").get("inheritors").asInt();

        // 公开不增加；后台全量增加 1
        assertThat(pubAfter).isEqualTo(pubBase);
        assertThat(adminAfter).isEqualTo(adminBase + 1);
    }

    @Test
    @DisplayName("B3c. 待审核资讯不计入公开资讯数，但计入后台全量")
    void pendingArticleExcludedFromPublic() throws Exception {
        String adminToken = login("admin");
        int pubBase = getJson(get("/api/stats/overview"))
                .get("data").get("articles").asInt();
        int adminBase = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken))
                .get("data").get("articles").asInt();

        CmsArticle pending = new CmsArticle();
        pending.setTitle("待审核资讯统计测试" + System.nanoTime());
        pending.setAuditStatus(StatusConstants.Audit.PENDING);
        articleMapper.insert(pending);
        tempArticleIds.add(pending.getId());

        int pubAfter = getJson(get("/api/stats/overview"))
                .get("data").get("articles").asInt();
        int adminAfter = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken))
                .get("data").get("articles").asInt();

        assertThat(pubAfter).isEqualTo(pubBase);
        assertThat(adminAfter).isEqualTo(adminBase + 1);
    }

    @Test
    @DisplayName("B3d. 待审核/已取消活动不计入公开活动数，但计入后台全量")
    void unpublishedActivityExcludedFromPublic() throws Exception {
        String adminToken = login("admin");
        int pubBase = getJson(get("/api/stats/overview"))
                .get("data").get("activities").asInt();
        int adminBase = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken))
                .get("data").get("activities").asInt();

        LocalDateTime now = LocalDateTime.now();
        // 公开口径只认报名中 status=1；待审核(0)不计入公开活动数
        IchActivity pending = new IchActivity();
        pending.setTitle("待审核活动" + System.nanoTime());
        pending.setStatus(0);
        pending.setStartTime(now.plusDays(1));
        pending.setEndTime(now.plusDays(2));
        activityMapper.insert(pending);
        tempActivityIds.add(pending.getId());

        int pubAfter = getJson(get("/api/stats/overview"))
                .get("data").get("activities").asInt();
        int adminAfter = getJson(get("/api/admin/stats/overview")
                .header("Authorization", "Bearer " + adminToken))
                .get("data").get("activities").asInt();

        // 公开不增加（待审核0 ≠ 报名中1）；后台全量 +1
        assertThat(pubAfter).isEqualTo(pubBase);
        assertThat(adminAfter).isEqualTo(adminBase + 1);
    }

    // ==================== B2. 驳回重提证件号唯一 ====================

    @Test
    @DisplayName("B2. 驳回后本人沿用原证件号可重提；与他人证件号冲突被拒绝(409)")
    void rejectedResubmitCertNoUnique() {
        // 用户A：驳回档案，证件号 CERT-A
        Long userA = insertTempUser("resubmit_a_" + System.nanoTime());
        IchInheritor recA = insertRejected(userA, "CERT-A");

        // 本人沿用原号重提 → 成功，回到待审核
        Long newIdA = inheritorService.apply(userA, applyReq("重提甲", "CERT-A"));
        assertThat(newIdA).isEqualTo(recA.getId());
        assertThat(inheritorMapper.selectById(recA.getId()).getCertStatus())
                .isEqualTo(StatusConstants.CertStatus.PENDING);

        // 用户B：驳回档案，证件号 CERT-B；重提时改用 CERT-A → 冲突
        Long userB = insertTempUser("resubmit_b_" + System.nanoTime());
        IchInheritor recB = insertRejected(userB, "CERT-B");
        assertThatThrownBy(() -> inheritorService.apply(userB, applyReq("重提乙", "CERT-A")))
                .isInstanceOf(BizException.class)
                .matches(e -> ((BizException) e).getCode() == com.yuxi.ich.common.response.ResultCode.CONFLICT.getCode());
    }

    // ==================== B2/B3 构造辅助 ====================

    private Long insertTempUser(String username) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword("x");
        u.setNickname(username);
        u.setRoleId(1L);
        u.setStatus(1);
        userMapper.insert(u);
        tempUserIds.add(u.getId());
        return u.getId();
    }

    private IchInheritor insertRejected(Long userId, String certNo) {
        IchInheritor rec = new IchInheritor();
        rec.setUserId(userId);
        rec.setName("驳回档案" + certNo);
        rec.setLevel(2);
        rec.setRegion("测试地区");
        rec.setCertNo(certNo);
        rec.setCertStatus(StatusConstants.CertStatus.REJECTED);
        rec.setViewCount(0);
        inheritorMapper.insert(rec);
        tempInheritorIds.add(rec.getId());
        return rec;
    }

    private InheritorApplyRequest applyReq(String name, String certNo) {
        InheritorApplyRequest req = new InheritorApplyRequest();
        req.setName(name);
        req.setGender(1);
        req.setLevel(2);
        req.setRegion("测试地区");
        req.setCertNo(certNo);
        req.setCraftYears(5);
        return req;
    }

    // ==================== HTTP 辅助 ====================

    private String login(String username) throws Exception {
        return loginByName(username, null);
    }

    private String loginByName(String username, String password) throws Exception {
        String pwd = (password == null) ? "123456" : password;
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + pwd + "\"}"))
                .andReturn();
        JsonNode b = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    private Long registerUser(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"Abc12345\","
                        + "\"nickname\":\"" + username + "\"}"))
                .andReturn();
        JsonNode b = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b.get("code").asInt()).isEqualTo(200);
        Long id = b.get("data").asLong();
        tempUserIds.add(id);
        return id;
    }

    private void postFavorite(String token, Long targetId, boolean expectActive) throws Exception {
        postFavoriteType(token, 1, targetId, expectActive);
    }

    private void postFavoriteType(String token, int targetType, Long targetId, boolean expectActive) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/interaction/favorite")
                .header("Authorization", "Bearer " + token)
                .param("targetType", String.valueOf(targetType))
                .param("targetId", String.valueOf(targetId)))
                .andReturn();
        JsonNode b = om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b.get("code").asInt()).isEqualTo(200);
        assertThat(b.get("data").get("active").asBoolean()).isEqualTo(expectActive);
    }

    private boolean hasFavorite(String token, String url, Long targetId) throws Exception {
        JsonNode b = getJson(get(url).header("Authorization", "Bearer " + token));
        assertThat(b.get("code").asInt()).isEqualTo(200);
        JsonNode records = b.get("data").get("records");
        if (records == null || !records.isArray()) {
            return false;
        }
        for (JsonNode row : records) {
            JsonNode id = row.get("targetId") != null ? row.get("targetId") : row.get("id");
            if (id != null && id.asLong() == targetId) {
                return true;
            }
        }
        return false;
    }

    private JsonNode getJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        MvcResult r = mockMvc.perform(req).andReturn();
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}

package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.inheritor.service.InheritorService;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.security.TokenBlacklist;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 数据级权限集成测试：项目/活动的归属校验。
 *
 * <p>覆盖用户要求的全部场景：
 * <ul>
 *   <li>传承人 A 修改/删除传承人 B 的项目 → 403</li>
 *   <li>传承人 A 修改/删除传承人 B 的活动 → 403</li>
 *   <li>传承人 A 查询传承人 B 的活动报名名单 → 403</li>
 *   <li>传承人 A 对传承人 B 的活动签到核销 → 403</li>
 *   <li>传承人伪造 inheritorId 把新建内容挂到他人名下 → 归属仍为本人的已认证档案</li>
 *   <li>管理员对上述资源的合法管理仍然成功</li>
 *   <li>报名 / 取消 / 重复报名 / 名额限制回归</li>
 * </ul>
 *
 * <p>测试用账号：
 * <ul>
 *   <li>A：演示数据账号 inheritor（已认证传承人，档案 id=1）</li>
 *   <li>B：本类中注册新用户 → 申请传承人 → 管理员审核通过（已认证传承人）</li>
 *   <li>admin：演示数据管理员</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("数据级权限：项目/活动归属校验")
class OwnershipPermissionTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IchProjectMapper projectMapper;

    @Autowired
    private IchActivityMapper activityMapper;

    @Autowired
    private InheritorService inheritorService;

    @Autowired
    private TokenBlacklist tokenBlacklist;

    private final ObjectMapper om = new ObjectMapper();

    /** 演示数据账号：管理员 / 已认证传承人 A / 普通用户 */
    private String adminToken;
    private String tokenA;
    private String userToken;

    /** 传承人 B 的登录令牌与其已认证档案 id */
    private String tokenB;
    private Long bInheritorId;

    /** B 名下新建的项目/活动 id（每个测试独立创建） */
    private Long bProjectId;
    private Long bActivityId;

    @BeforeEach
    void setUp() throws Exception {
        tokenBlacklist.clear();
        adminToken = token("admin");
        tokenA = token("inheritor");
        userToken = token("user");

        // 注册 B → 申请传承人 → 管理员审核通过 → 重新登录（角色提升为 INHERITOR）
        String bName = "inhB_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        assertThat(call("POST", "/api/auth/register", null,
                "{\"username\":\"" + bName + "\",\"password\":\"abc12345\"}")
                .get("code").asInt()).isEqualTo(200);
        String bRawToken = loginToken(bName, "abc12345");
        String certNo = "CERT-B-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        JsonNode applied = call("POST", "/api/inheritor/apply", bRawToken,
                "{\"name\":\"测试传承人B\",\"gender\":1,\"level\":2,\"region\":\"测试地区\","
                        + "\"projectId\":1,\"craftYears\":5,\"certNo\":\"" + certNo
                        + "\",\"intro\":\"测试简介\",\"works\":\"测试作品\",\"contact\":\"13800000000\"}");
        assertThat(applied.get("code").asInt()).isEqualTo(200);
        bInheritorId = applied.get("data").asLong();
        assertThat(callStatus("PUT", "/api/inheritor/" + bInheritorId + "/audit?certStatus=1",
                adminToken, null)).isEqualTo(200);
        // 角色提升后需重新登录
        tokenB = loginToken(bName, "abc12345");

        // B 各建一个项目与活动作为「他人资源」测试基座
        bProjectId = bCreateProject();
        bActivityId = bCreateActivity();
    }

    // ==================== 1. 跨传承人越权（项目） ====================

    @Test
    @DisplayName("P1. 传承人A修改/删除传承人B的项目被拒绝(403)")
    void inheritorACannotManageInheritorBProject() throws Exception {
        String body = projectBody("越权修改B的项目", null);
        assertThat(callStatus("PUT", "/api/project/" + bProjectId, tokenA, body))
                .isEqualTo(403);
        assertThat(callStatus("DELETE", "/api/project/" + bProjectId, tokenA, null))
                .isEqualTo(403);
    }

    @Test
    @DisplayName("P2. 传承人不能修改/删除无归属(他人)的演示项目(403)")
    void inheritorCannotManageOrphanProject() throws Exception {
        // 演示项目 id=2 未绑定任何传承人档案（inheritor_id 为 NULL），不属于任何人
        String body = projectBody("越权修改演示项目", null);
        assertThat(callStatus("PUT", "/api/project/2", tokenA, body)).isEqualTo(403);
        assertThat(callStatus("DELETE", "/api/project/2", tokenA, null)).isEqualTo(403);
    }

    @Test
    @DisplayName("P3. 传承人A可以修改/删除自己创建的项目(200)")
    void inheritorACanManageOwnProject() throws Exception {
        // A（inheritor 账号，档案 id=1）自己新建项目
        JsonNode created = call("POST", "/api/project", tokenA,
                projectBody("A自己的项目", null));
        assertThat(created.get("code").asInt()).isEqualTo(200);
        Long ownProjectId = created.get("data").asLong();
        assertThat(projectMapper.selectById(ownProjectId).getInheritorId()).isEqualTo(1L);

        assertThat(callStatus("PUT", "/api/project/" + ownProjectId, tokenA,
                projectBody("A自己的项目-改", null))).isEqualTo(200);
        assertThat(callStatus("DELETE", "/api/project/" + ownProjectId, tokenA, null))
                .isEqualTo(200);
    }

    // ==================== 2. 跨传承人越权（活动） ====================

    @Test
    @DisplayName("A1. 传承人A修改/删除传承人B的活动被拒绝(403)")
    void inheritorACannotManageInheritorBActivity() throws Exception {
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId, tokenA,
                activityBody("越权修改B的活动", 10))).isEqualTo(403);
        assertThat(callStatus("DELETE", "/api/activity/" + bActivityId, tokenA, null))
                .isEqualTo(403);
    }

    @Test
    @DisplayName("A2. 传承人A不能修改传承人A自己的活动归属校验(反向：B不能动A的活动1)")
    void inheritorBCannotManageInheritorAActivity() throws Exception {
        // 演示活动 1 归属传承人档案 1（= A），B 无权修改/删除
        assertThat(callStatus("PUT", "/api/activity/1", tokenB, activityBody("B越权改活动1", 20)))
                .isEqualTo(403);
        assertThat(callStatus("DELETE", "/api/activity/1", tokenB, null)).isEqualTo(403);
    }

    @Test
    @DisplayName("A3. 传承人A查询传承人B的活动报名名单被拒绝(403)且响应不含报名人敏感信息")
    void inheritorACannotViewInheritorBSignupList() throws Exception {
        // 先让 B 的活动审核通过并由真实用户报名，使名单中存在可泄露的敏感数据
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId + "/audit?status=1",
                adminToken, null)).isEqualTo(200);
        assertThat(call("POST", "/api/activity/" + bActivityId + "/signup", userToken,
                "{\"realName\":\"核销测试员\",\"phone\":\"13900000000\"}")
                .get("code").asInt()).isEqualTo(200);

        // A 越权查询 → 403，且响应体不含报名人姓名/电话/报名编号等任何敏感数据
        String body = rawBody("GET", "/api/activity/" + bActivityId
                + "/signups?pageNum=1&pageSize=10", tokenA, null);
        JsonNode json = om.readTree(body);
        assertThat(json.get("code").asInt()).isEqualTo(403);
        assertThat(body).doesNotContain("核销测试员", "13900000000", "signupNo");
    }

    @Test
    @DisplayName("A4. 传承人A对传承人B的活动执行签到核销被拒绝(403)")
    void inheritorACannotCheckinInheritorBActivity() throws Exception {
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId
                + "/checkin?signupNo=FAKE-NO-001", tokenA, null)).isEqualTo(403);
    }

    @Test
    @DisplayName("A5. 普通用户(无传承人/管理员角色)访问报名名单接口被拒绝(403)")
    void plainUserCannotViewSignupList() throws Exception {
        // user 为普通用户（ROLE_USER），Controller @PreAuthorize 角色门槛直接拒绝
        assertThat(callStatus("GET", "/api/activity/" + bActivityId + "/signups"
                + "?pageNum=1&pageSize=10", userToken, null)).isEqualTo(403);
    }

    // ==================== 3. 伪造 inheritorId 挂靠 ====================

    @Test
    @DisplayName("F1. 传承人伪造inheritorId创建内容，归属仍被强制绑定为本人档案")
    void forgedInheritorIdIsOverridden() throws Exception {
        // B 创建项目时提交 inheritorId=1（A 的档案），后端必须覆盖为 B 的档案 id
        JsonNode proj = call("POST", "/api/project", tokenB,
                projectBody("伪造挂靠项目", 1L));
        assertThat(proj.get("code").asInt()).isEqualTo(200);
        Long projId = proj.get("data").asLong();
        assertThat(projectMapper.selectById(projId).getInheritorId()).isEqualTo(bInheritorId);

        // B 创建活动时提交 inheritorId=1，同样必须覆盖
        JsonNode act = call("POST", "/api/activity", tokenB,
                activityBodyWithInheritor("伪造挂靠活动", 1L));
        assertThat(act.get("code").asInt()).isEqualTo(200);
        Long actId = act.get("data").asLong();
        assertThat(activityMapper.selectById(actId).getInheritorId()).isEqualTo(bInheritorId);
    }

    @Test
    @DisplayName("F2. 未认证用户不能发布项目/活动(403)")
    void uncertifiedUserCannotPublish() throws Exception {
        String u = "plain_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        assertThat(call("POST", "/api/auth/register", null,
                "{\"username\":\"" + u + "\",\"password\":\"abc12345\"}")
                .get("code").asInt()).isEqualTo(200);
        String plainToken = loginToken(u, "abc12345");
        assertThat(callStatus("POST", "/api/project", plainToken, projectBody("普通用户项目", null)))
                .isEqualTo(403);
        assertThat(callStatus("POST", "/api/activity", plainToken, activityBody("普通用户活动", 5)))
                .isEqualTo(403);
    }

    // ==================== 4. 管理员合法管理 ====================

    @Test
    @DisplayName("M1. 管理员可审核/查看名单/核销/修改/删除传承人的项目与活动")
    void adminCanManageAllResources() throws Exception {
        // 审核通过 B 的活动（status=1 报名中）
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId + "/audit?status=1",
                adminToken, null)).isEqualTo(200);

        // 普通用户报名 B 的活动
        JsonNode signup = call("POST", "/api/activity/" + bActivityId + "/signup", userToken,
                "{\"realName\":\"核销测试员\",\"phone\":\"13900000000\"}");
        assertThat(signup.get("code").asInt()).isEqualTo(200);
        String signupNo = signup.get("data").get("signupNo").asText();

        // 管理员查看 B 活动的报名名单 → 200 且包含该报名记录与报名人姓名
        JsonNode list = call("GET", "/api/activity/" + bActivityId + "/signups"
                + "?pageNum=1&pageSize=10", adminToken, null);
        assertThat(list.get("code").asInt()).isEqualTo(200);
        assertThat(list.get("data").get("total").asLong()).isGreaterThanOrEqualTo(1L);
        boolean foundSignup = false;
        for (JsonNode rec : list.get("data").get("records")) {
            if ("核销测试员".equals(rec.path("realName").asText())) {
                foundSignup = true;
                break;
            }
        }
        assertThat(foundSignup).as("管理员名单应包含真实报名记录与姓名").isTrue();

        // 管理员核销
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId
                + "/checkin?signupNo=" + signupNo, adminToken, null)).isEqualTo(200);

        // 管理员修改/删除 B 的项目与活动
        assertThat(callStatus("PUT", "/api/project/" + bProjectId, adminToken,
                projectBody("管理员代改B的项目", null))).isEqualTo(200);
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId, adminToken,
                activityBody("管理员代改B的活动", 20))).isEqualTo(200);
        assertThat(callStatus("DELETE", "/api/project/" + bProjectId, adminToken, null))
                .isEqualTo(200);
        assertThat(callStatus("DELETE", "/api/activity/" + bActivityId, adminToken, null))
                .isEqualTo(200);
    }

    // ==================== 5. 回归：报名 / 取消 / 重复报名 / 名额 ====================

    @Test
    @DisplayName("R1. 报名-重复报名409-名额满-取消-重新报名 全链路回归")
    void signupRegression() throws Exception {
        // 使用本类新建的活动并经管理员审核通过，避免与其他测试类
        // 共用演示活动 3 产生状态耦合；quota=1 同时回归名额限制
        JsonNode act = call("POST", "/api/activity", tokenB,
                activityBody("R1独立活动-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6),
                        1));
        assertThat(act.get("code").asInt()).isEqualTo(200);
        Long activityId = act.get("data").asLong();
        assertThat(callStatus("PUT", "/api/activity/" + activityId + "/audit?status=1",
                adminToken, null)).isEqualTo(200);

        String other = "other_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        assertThat(call("POST", "/api/auth/register", null,
                "{\"username\":\"" + other + "\",\"password\":\"abc12345\"}")
                .get("code").asInt()).isEqualTo(200);
        String otherToken = loginToken(other, "abc12345");

        // user 报名成功
        JsonNode first = call("POST", "/api/activity/" + activityId + "/signup", userToken,
                "{\"realName\":\"回归用户\",\"phone\":\"13700000000\"}");
        assertThat(first.get("code").asInt()).isEqualTo(200);

        // 同用户重复报名 → 409
        assertThat(callStatus("POST", "/api/activity/" + activityId + "/signup", userToken,
                "{\"realName\":\"回归用户\",\"phone\":\"13700000000\"}")).isEqualTo(409);

        // 名额已满（quota=1），其他用户报名 → 409
        assertThat(callStatus("POST", "/api/activity/" + activityId + "/signup", otherToken,
                "{\"realName\":\"其他用户\",\"phone\":\"13600000000\"}")).isEqualTo(409);

        // user 取消报名 → 200
        assertThat(callStatus("DELETE", "/api/activity/" + activityId + "/signup",
                userToken, null)).isEqualTo(200);

        // 重新报名成功（复用已取消记录）
        JsonNode again = call("POST", "/api/activity/" + activityId + "/signup", userToken,
                "{\"realName\":\"回归用户\",\"phone\":\"13700000000\"}");
        assertThat(again.get("code").asInt()).isEqualTo(200);
    }

    @Test
    @DisplayName("R2. 传承人A仍可查看/核销自己主办活动的名单")
    void ownActivityListAndCheckinStillWork() throws Exception {
        // 管理员把 B 的活动审核通过，user 报名，A 不能核销；B 可以
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId + "/audit?status=1",
                adminToken, null)).isEqualTo(200);
        JsonNode signup = call("POST", "/api/activity/" + bActivityId + "/signup", userToken,
                "{\"realName\":\"核销测试员\",\"phone\":\"13900000000\"}");
        assertThat(signup.get("code").asInt()).isEqualTo(200);
        String signupNo = signup.get("data").get("signupNo").asText();

        // B 查看自己活动名单 → 200
        assertThat(callStatus("GET", "/api/activity/" + bActivityId + "/signups"
                + "?pageNum=1&pageSize=10", tokenB, null)).isEqualTo(200);
        // B 核销自己活动 → 200
        assertThat(callStatus("PUT", "/api/activity/" + bActivityId
                + "/checkin?signupNo=" + signupNo, tokenB, null)).isEqualTo(200);
    }

    // ==================== 工具方法 ====================

    private JsonNode call(String method, String url, String token, String body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            case "DELETE" -> delete(url);
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

    /**
     * 返回响应体中的业务 code（本项目统一 HTTP 200 + body.code 承载错误：
     * 403 越权 / 409 冲突 / 400 参数错误）。
     */
    private int callStatus(String method, String url, String token, String body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            case "DELETE" -> delete(url);
            default -> get(url);
        };
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req = req.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        MvcResult r = mockMvc.perform(req).andReturn();
        String content = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        return om.readTree(content).get("code").asInt();
    }

    /** 返回完整响应体字符串（不解析），用于断言越权响应不泄露敏感数据 */
    private String rawBody(String method, String url, String token, String body) throws Exception {
        var req = switch (method) {
            case "POST" -> post(url);
            case "PUT" -> put(url);
            case "DELETE" -> delete(url);
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

    private String token(String username) throws Exception {
        return loginToken(username, "123456");
    }

    private String loginToken(String username, String password) throws Exception {
        JsonNode b = call("POST", "/api/auth/login", null,
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    /** B 创建项目，返回项目 id */
    private Long bCreateProject() throws Exception {
        JsonNode r = call("POST", "/api/project", tokenB,
                projectBody("B的项目-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6),
                        null));
        assertThat(r.get("code").asInt()).isEqualTo(200);
        return r.get("data").asLong();
    }

    /** B 创建活动，返回活动 id */
    private Long bCreateActivity() throws Exception {
        JsonNode r = call("POST", "/api/activity", tokenB,
                activityBody("B的活动-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6),
                        10));
        assertThat(r.get("code").asInt()).isEqualTo(200);
        return r.get("data").asLong();
    }

    private String projectBody(String name, Long fakeInheritorId) {
        String code = "CODE-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String inh = fakeInheritorId == null ? "" : ",\"inheritorId\":" + fakeInheritorId;
        return "{\"projectName\":\"" + name + "\",\"projectCode\":\"" + code
                + "\",\"categoryId\":8,\"level\":1,\"region\":\"测试地区\","
                + "\"batchNo\":5,\"announceYear\":2025,\"type\":\"新增项目\","
                + "\"summary\":\"测试摘要\",\"content\":\"测试正文内容\""
                + inh + "}";
    }

    private String activityBody(String title, int quota) {
        return "{\"title\":\"" + title + "\",\"activityType\":2,\"projectId\":1,"
                + "\"content\":\"测试活动内容\",\"startTime\":\"2030-01-01T10:00:00\","
                + "\"endTime\":\"2030-01-01T12:00:00\",\"address\":\"测试地址\","
                + "\"quota\":" + quota + "}";
    }

    private String activityBodyWithInheritor(String title, Long fakeInheritorId) {
        return "{\"title\":\"" + title + "\",\"activityType\":2,\"projectId\":1,"
                + "\"content\":\"测试活动内容\",\"startTime\":\"2030-01-01T10:00:00\","
                + "\"endTime\":\"2030-01-01T12:00:00\",\"address\":\"测试地址\","
                + "\"quota\":10,\"inheritorId\":" + fakeInheritorId + "}";
    }
}

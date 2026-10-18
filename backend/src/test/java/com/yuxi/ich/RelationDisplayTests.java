package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 项目—传承人关联表（ich_project_inheritor）只读展示集成测试。
 *
 * <p>通过真实 Spring Security / Controller / Service / Mapper / H2 链路验证，
 * 不 mock 被测校验逻辑。所有临时关系与临时档案在 {@link Transactional} 内插入，
 * 方法结束自动回滚，不污染演示数据与其他测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>一个项目关联多个传承人；</li>
 *   <li>一个传承人关联多个项目；</li>
 *   <li>无任何关联时返回空数组 []；</li>
 *   <li>未认证传承人不出现在公开关联列表；</li>
 *   <li>未审核 / 已软删项目不出现在公开关联列表；</li>
 *   <li>关联关系只用于展示，不授予项目管理权限。</li>
 * </ul>
 *
 * <p>红线：本测试不修改 checkCanManage / checkActivityHost / resolveInheritorIdForWrite，
 * 也不把关联关系写回 ich_project.inheritor_id 或 ich_inheritor.project_id。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("关联表只读展示：项目-传承人双向列表与权限隔离")
class RelationDisplayTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper om = new ObjectMapper();

    // ==================== 1. 既有演示关系真实命中（非 mock） ====================

    @Test
    @DisplayName("R0. 演示既有关系(1,1)在项目详情与传承人详情双向可见")
    void demoRelationVisibleBothWays() throws Exception {
        JsonNode project = getJson("/api/project/1");
        assertThat(project.get("code").asInt()).isEqualTo(200);
        JsonNode relatedInheritors = project.get("data").get("relatedInheritors");
        assertThat(relatedInheritors).isNotNull();
        assertThat(idSet(relatedInheritors)).contains(1L);

        JsonNode inheritor = getJson("/api/inheritor/1");
        assertThat(inheritor.get("code").asInt()).isEqualTo(200);
        JsonNode relatedProjects = inheritor.get("data").get("relatedProjects");
        assertThat(relatedProjects).isNotNull();
        assertThat(idSet(relatedProjects)).contains(1L);
    }

    // ==================== 2. 一项目多传承人 ====================

    @Test
    @DisplayName("R1. 一个项目可关联多名已认证传承人，列表全部返回")
    void oneProjectHasManyInheritors() throws Exception {
        // 项目2（玉溪花灯戏）临时关联档案1、档案2，均为已认证
        insertRelation(2L, 1L);
        insertRelation(2L, 2L);

        JsonNode project = getJson("/api/project/2");
        assertThat(project.get("code").asInt()).isEqualTo(200);
        JsonNode list = project.get("data").get("relatedInheritors");
        assertThat(idSet(list)).containsExactlyInAnyOrder(1L, 2L);
    }

    // ==================== 3. 一传承人多项目 ====================

    @Test
    @DisplayName("R2. 一个传承人可关联多个已发布项目，列表全部返回")
    void oneInheritorHasManyProjects() throws Exception {
        // 既有(1,1)；临时把项目2也关联到档案1
        insertRelation(2L, 1L);

        JsonNode inheritor = getJson("/api/inheritor/1");
        assertThat(inheritor.get("code").asInt()).isEqualTo(200);
        JsonNode list = inheritor.get("data").get("relatedProjects");
        assertThat(idSet(list)).contains(1L, 2L);
    }

    // ==================== 4. 无关联返回空数组 ====================

    @Test
    @DisplayName("R3. 项目/传承人无关联时返回空数组 []，字段不缺失")
    void noRelationReturnsEmptyArray() throws Exception {
        // 项目7（景德镇）无任何关联
        JsonNode project = getJson("/api/project/7");
        assertThat(project.get("code").asInt()).isEqualTo(200);
        assertThat(project.get("data").get("relatedInheritors").isArray()).isTrue();
        assertThat(project.get("data").get("relatedInheritors")).isEmpty();

        // 临时新建一名已认证传承人，且不建立任何项目关联
        Long tmpInheritorId = insertCertifiedInheritor();
        JsonNode inheritor = getJson("/api/inheritor/" + tmpInheritorId);
        assertThat(inheritor.get("code").asInt()).isEqualTo(200);
        assertThat(inheritor.get("data").get("relatedProjects").isArray()).isTrue();
        assertThat(inheritor.get("data").get("relatedProjects")).isEmpty();
    }

    // ==================== 5. 未认证传承人被过滤 ====================

    @Test
    @DisplayName("R4. 待审核/未认证传承人不出现在项目关联列表")
    void uncertifiedInheritorIsHidden() throws Exception {
        Long tmpId = insertUncertifiedInheritor();
        insertRelation(2L, tmpId);

        JsonNode project = getJson("/api/project/2");
        assertThat(project.get("code").asInt()).isEqualTo(200);
        assertThat(idSet(project.get("data").get("relatedInheritors")))
                .doesNotContain(tmpId);
    }

    // ==================== 6. 未审核 / 软删项目被过滤 ====================

    @Test
    @DisplayName("R5. 待审核项目与已软删项目不出现在传承人关联列表")
    void unpublishedAndDeletedProjectsAreHidden() throws Exception {
        // 项目14为待审核(audit_status=0)；临时把它关联到档案1
        insertRelation(14L, 1L);
        // 项目7临时软删(deleted=1)，再建立关联
        jdbc.update("UPDATE ich_project SET deleted = 1 WHERE id = 7");
        insertRelation(7L, 1L);

        JsonNode inheritor = getJsonUnchecked("/api/inheritor/1");
        assertThat(inheritor.get("code").asInt()).isEqualTo(200);
        java.util.Set<Long> ids = idSet(inheritor.get("data").get("relatedProjects"));
        assertThat(ids).doesNotContain(14L, 7L);
    }

    // ==================== 7. 关联不授予管理权限 ====================

    @Test
    @DisplayName("R6. 仅存在关联关系但无归属的项目，传承人仍无权修改(403)")
    void relationDoesNotGrantManagePermission() throws Exception {
        // 项目2的 inheritor_id 为 NULL（无归属），这里仅临时建立展示关联，
        // 不得因此让档案1获得管理权限。
        insertRelation(2L, 1L);
        String inheritorToken = loginToken("inheritor", "123456");

        String body = "{\"projectName\":\"越权改项目\",\"projectCode\":\"REL-PERM-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8)
                + "\",\"categoryId\":8,\"level\":1,\"region\":\"测试地区\","
                + "\"summary\":\"测试摘要\",\"content\":\"x\"}";
        var req = put("/api/project/2")
                .header("Authorization", "Bearer " + inheritorToken)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(body);
        String raw = mockMvc.perform(req).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode resp = om.readTree(raw);
        assertThat(resp.get("code").asInt()).isEqualTo(403);
    }

    // ==================== 工具方法 ====================

    private JsonNode getJson(String url) throws Exception {
        return om.readTree(mockMvc.perform(get(url)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private JsonNode getJsonUnchecked(String url) throws Exception {
        return om.readTree(mockMvc.perform(get(url)).andReturn()
                .getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String loginToken(String username, String password) throws Exception {
        JsonNode b = om.readTree(mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/auth/login")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").get("accessToken").asText();
    }

    private void insertRelation(Long projectId, Long inheritorId) {
        jdbc.update("INSERT INTO ich_project_inheritor (project_id, inheritor_id, relation) VALUES (?, ?, '传承')",
                projectId, inheritorId);
    }

    private Long insertCertifiedInheritor() {
        String certNo = "CERT-TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        org.springframework.jdbc.support.GeneratedKeyHolder kh =
                new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(
                    "INSERT INTO ich_inheritor (name, level, region, cert_no, cert_status) "
                            + "VALUES (?, ?, ?, ?, 1)", new String[]{"id"});
            ps.setString(1, "临时已认证传承人");
            ps.setInt(2, 2);
            ps.setString(3, "测试地区");
            ps.setString(4, certNo);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    private Long insertUncertifiedInheritor() {
        String certNo = "CERT-PEND-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        org.springframework.jdbc.support.GeneratedKeyHolder kh =
                new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(conn -> {
            var ps = conn.prepareStatement(
                    "INSERT INTO ich_inheritor (name, level, region, cert_no, cert_status) "
                            + "VALUES (?, ?, ?, ?, 0)", new String[]{"id"});
            ps.setString(1, "待审核传承人");
            ps.setInt(2, 2);
            ps.setString(3, "测试地区");
            ps.setString(4, certNo);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }

    private java.util.Set<Long> idSet(JsonNode array) {
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        if (array != null && array.isArray()) {
            for (JsonNode node : array) {
                JsonNode id = node.get("id");
                if (id != null && !id.isNull()) {
                    ids.add(id.asLong());
                }
            }
        }
        return ids;
    }
}

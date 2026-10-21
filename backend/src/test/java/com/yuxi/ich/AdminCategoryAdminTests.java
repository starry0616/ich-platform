package com.yuxi.ich;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.project.entity.IchCategory;
import com.yuxi.ich.project.mapper.IchCategoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * 后台类别管理测试（C1）。
 *
 * <p>覆盖：管理员新增/编辑/删除类别、普通用户被 403 拒绝、被项目占用的类别禁止删除。
 * 仅用 H2 内存库与演示账号，临时类别用后即删，不连接 MySQL。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("后台类别管理 CRUD 与权限测试")
class AdminCategoryAdminTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IchCategoryMapper categoryMapper;

    private final ObjectMapper om = new ObjectMapper();
    private final List<Long> tempCategoryIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long id : tempCategoryIds) {
            categoryMapper.deleteById(id);
        }
        tempCategoryIds.clear();
    }

    @Test
    @DisplayName("C1a. 管理员可新增并在列表看到，可编辑；普通用户访问被拒(403)")
    void adminCanCreateAndUpdate_otherForbidden() throws Exception {
        String name = "测试门类_" + System.nanoTime();
        String adminToken = login("admin");

        // 新增
        MvcResult create = mockMvc.perform(post("/api/admin/category")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"" + name + "\",\"categoryCode\":\"T\","
                        + "\"parentId\":0,\"sortNo\":99,\"status\":1}")).andReturn();
        JsonNode createJson = body(create);
        assertThat(createJson.get("code").asInt()).isEqualTo(200);
        Long newId = createJson.get("data").asLong();
        tempCategoryIds.add(newId);

        // 列表可见
        assertThat(listNames(adminToken)).contains(name);

        // 编辑改名
        String renamed = name + "_改";
        MvcResult upd = mockMvc.perform(put("/api/admin/category/" + newId)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"" + renamed + "\",\"parentId\":0,"
                        + "\"sortNo\":99,\"status\":1}")).andReturn();
        assertThat(body(upd).get("code").asInt()).isEqualTo(200);
        assertThat(listNames(adminToken)).contains(renamed);

        // 普通用户访问列表 → 403
        String userToken = login("user");
        MvcResult forbidden = mockMvc.perform(get("/api/admin/category/list")
                .header("Authorization", "Bearer " + userToken)).andReturn();
        assertThat(body(forbidden).get("code").asInt()).isEqualTo(403);
    }

    @Test
    @DisplayName("C1b. 删除自建空类别成功；被项目占用的类别删除返回 409")
    void deleteSelfCreatedOk_occupiedBlocked() throws Exception {
        String adminToken = login("admin");

        // 自建空类别 → 删除成功
        String name = "待删门类_" + System.nanoTime();
        MvcResult create = mockMvc.perform(post("/api/admin/category")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"" + name + "\",\"parentId\":0,\"status\":1}"))
                .andReturn();
        Long newId = body(create).get("data").asLong();
        tempCategoryIds.add(newId);

        MvcResult del = mockMvc.perform(delete("/api/admin/category/" + newId)
                .header("Authorization", "Bearer " + adminToken)).andReturn();
        assertThat(body(del).get("code").asInt()).isEqualTo(200);
        assertThat(listNames(adminToken)).doesNotContain(name);

        // 被项目占用的类别（传统技艺）→ 409，不得删除
        Long occupiedId = categoryMapper.selectList(null).stream()
                .filter(c -> "传统技艺".equals(c.getCategoryName()))
                .map(IchCategory::getId)
                .findFirst().orElse(null);
        if (occupiedId != null) {
            MvcResult blocked = mockMvc.perform(delete("/api/admin/category/" + occupiedId)
                    .header("Authorization", "Bearer " + adminToken)).andReturn();
            assertThat(body(blocked).get("code").asInt()).isEqualTo(409);
        }
    }

    @Test
    @DisplayName("C1c. 重名类别被拒(409)；更新时把自身设为父级被拒(400)")
    void duplicateNameAndSelfReferenceRejected() throws Exception {
        String adminToken = login("admin");
        String base = "重门类_" + System.nanoTime();
        Long first = createCategory(adminToken, base, 0L);
        tempCategoryIds.add(first);

        // 重名 → 409
        MvcResult dup = mockMvc.perform(post("/api/admin/category")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"" + base + "\",\"parentId\":0,\"status\":1}"))
                .andReturn();
        assertThat(body(dup).get("code").asInt()).isEqualTo(409);

        // 自引用：把自身设为父级 → 400
        MvcResult self = mockMvc.perform(put("/api/admin/category/" + first)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"" + base + "\",\"parentId\":" + first + ",\"status\":1}"))
                .andReturn();
        assertThat(body(self).get("code").asInt()).isEqualTo(400);
    }

    @Test
    @DisplayName("C1d. 间接循环层级被拒(400)；普通用户 POST/PUT/DELETE 均 403")
    void indirectCycleRejectedAndUserForbidden() throws Exception {
        String adminToken = login("admin");
        Long parent = createCategory(adminToken, "循环父_" + System.nanoTime(), 0L);
        Long child = createCategory(adminToken, "循环子_" + System.nanoTime(), parent);
        tempCategoryIds.add(parent);
        tempCategoryIds.add(child);

        // 父挂到子 → 形成间接循环，400
        MvcResult cycle = mockMvc.perform(put("/api/admin/category/" + parent)
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"循环父\",\"parentId\":" + child + ",\"status\":1}"))
                .andReturn();
        assertThat(body(cycle).get("code").asInt()).isEqualTo(400);

        // 普通用户写操作全部 403
        String userToken = login("user");
        assertThat(body(mockMvc.perform(post("/api/admin/category")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"x\",\"parentId\":0}")).andReturn())
                .get("code").asInt()).isEqualTo(403);
        assertThat(body(mockMvc.perform(put("/api/admin/category/" + parent)
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"x\",\"parentId\":0}")).andReturn())
                .get("code").asInt()).isEqualTo(403);
        assertThat(body(mockMvc.perform(delete("/api/admin/category/" + child)
                .header("Authorization", "Bearer " + userToken)).andReturn())
                .get("code").asInt()).isEqualTo(403);
    }

    // ==================== 工具 ====================

    private Long createCategory(String token, String name, Long parentId) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/admin/category")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"" + name + "\",\"parentId\":" + parentId + ",\"status\":1}"))
                .andReturn();
        JsonNode b = body(r);
        assertThat(b.get("code").asInt()).isEqualTo(200);
        return b.get("data").asLong();
    }

    private List<String> listNames(String adminToken) throws Exception {
        MvcResult r = mockMvc.perform(get("/api/admin/category/list")
                .header("Authorization", "Bearer " + adminToken)).andReturn();
        JsonNode data = body(r).get("data");
        List<String> names = new ArrayList<>();
        if (data != null && data.isArray()) {
            for (JsonNode row : data) {
                names.add(row.get("categoryName").asText());
            }
        }
        return names;
    }

    private JsonNode body(MvcResult r) throws Exception {
        return om.readTree(r.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String login(String username) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"123456\"}"))
                .andReturn();
        return body(r).get("data").get("accessToken").asText();
    }
}

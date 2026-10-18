package com.yuxi.ich.project.controller;

import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.project.dto.ProjectQueryParam;
import com.yuxi.ich.project.dto.ProjectSaveRequest;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 非遗项目接口。
 *
 * <p>对应《系统设计说明书》表 6-3 的接口 14—20。
 * Controller 只负责：
 * <ol>
 *   <li>接收与绑定参数</li>
 *   <li>声明访问权限（{@code @PreAuthorize}）</li>
 *   <li>包装统一响应</li>
 * </ol>
 * 业务规则全部在 {@link ProjectService} 中，便于单元测试。
 */
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * 项目分页列表（公开接口）。
     *
     * <p>支持多条件组合筛选，示例：
     * <pre>
     * GET /api/project/list?categoryId=8&amp;level=1&amp;region=云南&amp;keyword=陶瓷&amp;pageNum=1&amp;pageSize=12
     * </pre>
     */
    @GetMapping("/list")
    public Result<PageResult<IchProject>> list(ProjectQueryParam query) {
        return Result.success(projectService.pageForFront(query));
    }

    /**
     * 热门项目（公开接口）。
     *
     * @param limit 返回条数，默认 6，最大 20
     */
    @GetMapping("/hot")
    public Result<List<IchProject>> hot(
            @RequestParam(value = "limit", defaultValue = "6") int limit) {
        return Result.success(projectService.hot(limit));
    }

    /**
     * 项目详情（公开接口）。
     *
     * <p>仅返回已发布内容；未发布或已删除一律返回 404，
     * 不对外暴露「存在但未发布」这一事实。
     */
    @GetMapping("/{id}")
    public Result<IchProject> detail(@PathVariable("id") Long id) {
        return Result.success(projectService.getDetail(id, true, true));
    }

    /**
     * 各类别项目数统计（公开接口），用于首页图表。
     */
    @GetMapping("/stats/category")
    public Result<List<?>> categoryStats() {
        return Result.success(projectService.countByCategory());
    }

    // ==================== 以下为需要登录的写操作 ====================

    /**
     * 新增项目。
     *
     * <p>传承人与管理员均可提交；提交后进入待审核状态。
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Long> create(@Valid @RequestBody ProjectSaveRequest request) {
        return Result.success("提交成功，等待审核", projectService.create(request));
    }

    /**
     * 修改项目。修改后重新进入待审核。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ProjectSaveRequest request) {
        request.setId(id);
        projectService.update(request);
        return Result.success("修改成功，等待审核", null);
    }

    /**
     * 删除项目（逻辑删除，数据保留可追溯）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        projectService.delete(id);
        return Result.success("删除成功", null);
    }

    /**
     * 管理员视角的分页查询：可查看待审核、已下架等全部状态。
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<IchProject>> adminList(ProjectQueryParam query) {
        return Result.success(projectService.pageForAdmin(query));
    }

    /**
     * 内容审核：通过 / 驳回 / 下架。
     *
     * @param auditStatus 1 发布，2 下架，3 驳回
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> audit(@PathVariable("id") Long id,
                              @RequestParam("auditStatus") Integer auditStatus,
                              @RequestParam(value = "remark", required = false) String remark) {
        projectService.audit(id, auditStatus, remark);
        return Result.success("审核完成", null);
    }
}

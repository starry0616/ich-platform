package com.yuxi.ich.article.controller;

import com.yuxi.ich.article.dto.ArticleQueryParam;
import com.yuxi.ich.article.dto.ArticleSaveRequest;
import com.yuxi.ich.article.entity.CmsArticle;
import com.yuxi.ich.article.entity.CmsArticleCategory;
import com.yuxi.ich.article.service.ArticleService;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
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
 * 文化资讯接口（对应说明书表 6-4 接口 26—29）。
 */
@RestController
@RequestMapping("/api/article")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    // ==================== 公开查询 ====================

    /**
     * 资讯分页列表（公开，仅已发布）。
     * <pre>
     * GET /api/article/list?categoryId=1&amp;keyword=非遗&amp;pageNum=1&amp;pageSize=10
     * </pre>
     */
    @GetMapping("/list")
    public Result<PageResult<CmsArticle>> list(ArticleQueryParam query) {
        return Result.success(articleService.pageForFront(query));
    }

    /** 资讯分类列表（公开） */
    @GetMapping("/categories")
    public Result<List<CmsArticleCategory>> categories() {
        return Result.success(articleService.listCategories());
    }

    /** 资讯详情（公开）。注意路径要在 /{id} 之前，避免被路径变量吞掉 */
    @GetMapping("/detail/{id}")
    public Result<CmsArticle> detail(@PathVariable("id") Long id) {
        return Result.success(articleService.getDetail(id, true, true));
    }

    /**
     * 详情页的相关资讯推荐（公开）。
     *
     * @param projectId 关联的非遗项目 id
     * @param excludeId 需要排除的当前资讯 id
     */
    @GetMapping("/related")
    public Result<List<CmsArticle>> related(@RequestParam("projectId") Long projectId,
                                            @RequestParam(value = "excludeId", required = false)
                                            Long excludeId) {
        return Result.success(articleService.related(projectId, excludeId));
    }

    // ==================== 投稿与维护 ====================

    /** 我的投稿列表 */
    @GetMapping("/mine")
    public Result<PageResult<CmsArticle>> mine(ArticleQueryParam query) {
        return Result.success(articleService.pageMine(query));
    }

    /**
     * 发布 / 投稿资讯（传承人或管理员）。
     *
     * <p>提交后进入待审核，前台不可见。
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Long> create(@Valid @RequestBody ArticleSaveRequest request) {
        return Result.success("提交成功，等待审核", articleService.create(request));
    }

    /**
     * 修改资讯（作者本人或管理员）。
     *
     * <p>数据权限在 Service 层校验，篡改 id 改他人稿件会返回 403。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> update(@PathVariable("id") Long id,
                               @Valid @RequestBody ArticleSaveRequest request) {
        request.setId(id);
        articleService.update(request);
        return Result.success("修改成功，等待审核", null);
    }

    /** 删除资讯（作者本人或管理员，逻辑删除） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        articleService.delete(id);
        return Result.success("删除成功", null);
    }

    // ==================== 后台管理 ====================

    /** 后台分页：可查看待审核等全部状态（管理员） */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<CmsArticle>> adminList(ArticleQueryParam query) {
        return Result.success(articleService.pageForAdmin(query));
    }

    /**
     * 资讯审核（管理员）。
     *
     * @param auditStatus 1 发布，2 下架，3 驳回
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> audit(@PathVariable("id") Long id,
                              @RequestParam("auditStatus") Integer auditStatus,
                              @RequestParam(value = "remark", required = false) String remark) {
        articleService.audit(id, auditStatus, remark);
        return Result.success("审核完成", null);
    }

    /** 设置 / 取消置顶（管理员） */
    @PutMapping("/{id}/top")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> setTop(@PathVariable("id") Long id,
                               @RequestParam("isTop") Integer isTop) {
        articleService.setTop(id, isTop);
        return Result.success("设置成功", null);
    }
}

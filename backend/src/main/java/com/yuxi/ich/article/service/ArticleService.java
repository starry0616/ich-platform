package com.yuxi.ich.article.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.article.dto.ArticleQueryParam;
import com.yuxi.ich.article.dto.ArticleSaveRequest;
import com.yuxi.ich.article.entity.CmsArticle;
import com.yuxi.ich.article.entity.CmsArticleCategory;
import com.yuxi.ich.article.mapper.CmsArticleCategoryMapper;
import com.yuxi.ich.article.mapper.CmsArticleMapper;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.user.service.UserService;
import com.yuxi.ich.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文化资讯服务。
 *
 * <p>与项目模块的差异在于<b>作者归属</b>：资讯由具体用户发布，
 * 因此除了审核状态外，还要做作者级别的数据权限校验——
 * 传承人只能改自己的稿件，管理员不受限。
 */
@Service
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);

    /** 相关资讯推荐条数 */
    private static final int RELATED_LIMIT = 5;

    private final CmsArticleMapper articleMapper;
    private final CmsArticleCategoryMapper categoryMapper;
    private final IchProjectMapper projectMapper;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public ArticleService(CmsArticleMapper articleMapper,
                          CmsArticleCategoryMapper categoryMapper,
                          IchProjectMapper projectMapper,
                          AuditLogService auditLogService,
                          UserService userService) {
        this.articleMapper = articleMapper;
        this.categoryMapper = categoryMapper;
        this.projectMapper = projectMapper;
        this.auditLogService = auditLogService;
        this.userService = userService;
    }

    // ==================== 查询 ====================

    /** 前台分页：只看已发布 */
    public PageResult<CmsArticle> pageForFront(ArticleQueryParam query) {
        query.setAuditStatus(StatusConstants.Audit.PUBLISHED);
        return doPage(query);
    }

    /** 后台分页：可看全部状态 */
    public PageResult<CmsArticle> pageForAdmin(ArticleQueryParam query) {
        return doPage(query);
    }

    private PageResult<CmsArticle> doPage(ArticleQueryParam query) {
        long offset = (query.getPageNum() - 1) * query.getPageSize();
        List<CmsArticle> records =
                articleMapper.selectArticlePage(query, offset, query.getPageSize());
        long total = articleMapper.countArticle(query);
        return new PageResult<>(total, records, query.getPageNum(), query.getPageSize());
    }

    /** 我的投稿列表 */
    public PageResult<CmsArticle> pageMine(ArticleQueryParam query) {
        query.setAuthorId(SecurityUtils.getUserId());
        return doPage(query);
    }

    /**
     * 详情。
     *
     * @param onlyPublished 前台传 true
     * @param countView     是否累加浏览量
     */
    public CmsArticle getDetail(Long id, boolean onlyPublished, boolean countView) {
        CmsArticle article = articleMapper.selectArticleDetail(id);
        if (article == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在或已下架");
        }
        if (onlyPublished && article.getAuditStatus() != null
                && article.getAuditStatus() != StatusConstants.Audit.PUBLISHED) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在或已下架");
        }
        if (countView) {
            articleMapper.incrementViewCount(id);
            // 浏览历史（登录用户）
            userService.recordBrowse(StatusConstants.Interaction.TARGET_ARTICLE, id);
        }
        return article;
    }

    /** 详情页的相关资讯推荐 */
    public List<CmsArticle> related(Long projectId, Long excludeId) {
        if (projectId == null) {
            return List.of();
        }
        return articleMapper.selectRelated(projectId, excludeId == null ? -1L : excludeId,
                RELATED_LIMIT);
    }

    /** 资讯分类列表 */
    public List<CmsArticleCategory> listCategories() {
        return categoryMapper.selectList(
                Wrappers.<CmsArticleCategory>lambdaQuery()
                        .orderByAsc(CmsArticleCategory::getSortNo));
    }

    // ==================== 发布与修改 ====================

    /**
     * 新增资讯。
     *
     * <p>提交后进入待审核；置顶仅管理员可设置，普通传承人传入的 isTop 会被忽略。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ArticleSaveRequest req) {
        checkCategoryExists(req.getCategoryId());
        checkProjectExists(req.getProjectId());

        CmsArticle entity = new CmsArticle();
        apply(req, entity);
        entity.setAuthorId(SecurityUtils.getUserId());
        entity.setAuditStatus(StatusConstants.Audit.PENDING);
        entity.setViewCount(0);
        entity.setIsTop(SecurityUtils.isAdmin() ? nvl(req.getIsTop()) : 0);
        entity.setPublishTime(null);
        articleMapper.insert(entity);
        log.info("资讯提交：id={} author={} title={}",
                entity.getId(), entity.getAuthorId(), entity.getTitle());
        return entity.getId();
    }

    /**
     * 修改资讯。
     *
     * <p>两层校验：数据权限（只能改自己的，管理员除外）+ 状态回退（重新进审核）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(ArticleSaveRequest req) {
        if (req.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少资讯 id");
        }
        CmsArticle exist = articleMapper.selectById(req.getId());
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在");
        }
        // 数据级权限：防篡改 id 改他人稿件
        SecurityUtils.checkOwnership(exist.getAuthorId());

        checkCategoryExists(req.getCategoryId());
        checkProjectExists(req.getProjectId());

        CmsArticle update = new CmsArticle();
        update.setId(req.getId());
        apply(req, update);
        // 修改后重新进入待审核，避免绕过审核改已发布内容
        update.setAuditStatus(StatusConstants.Audit.PENDING);
        if (SecurityUtils.isAdmin()) {
            update.setIsTop(nvl(req.getIsTop()));
        }
        articleMapper.updateById(update);
    }

    /** 删除（逻辑删除），作者本人或管理员 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CmsArticle exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在");
        }
        SecurityUtils.checkOwnership(exist.getAuthorId());
        articleMapper.deleteById(id);
    }

    // ==================== 审核 ====================

    /**
     * 内容审核。
     *
     * <p>审核通过时写入发布时间，列表默认按发布时间排序才有意义。
     *
     * @param auditStatus 1 发布，2 下架，3 驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Integer auditStatus, String remark) {
        if (auditStatus == null || auditStatus < 1 || auditStatus > 3) {
            throw new BizException(ResultCode.PARAM_ERROR, "审核状态不合法");
        }
        CmsArticle exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在");
        }
        CmsArticle update = new CmsArticle();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(remark);
        if (auditStatus == StatusConstants.Audit.PUBLISHED) {
            // 首次发布才写发布时间；重新发布不覆盖原时间
            update.setPublishTime(exist.getPublishTime() == null
                    ? LocalDateTime.now() : exist.getPublishTime());
        }
        articleMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_ARTICLE, id,
                auditStatus == 1 ? AuditLogService.ACTION_PASS
                        : auditStatus == 2 ? AuditLogService.ACTION_OFFLINE
                        : AuditLogService.ACTION_REJECT,
                remark);
    }

    /** 管理员设置 / 取消置顶 */
    @Transactional(rollbackFor = Exception.class)
    public void setTop(Long id, Integer isTop) {
        if (isTop == null || (isTop != 0 && isTop != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "置顶取值只能是 0 或 1");
        }
        if (articleMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "资讯不存在");
        }
        CmsArticle update = new CmsArticle();
        update.setId(id);
        update.setIsTop(isTop);
        articleMapper.updateById(update);
    }

    // ==================== 内部工具 ====================

    private void apply(ArticleSaveRequest req, CmsArticle entity) {
        entity.setTitle(req.getTitle());
        entity.setCategoryId(req.getCategoryId());
        entity.setCoverUrl(req.getCoverUrl());
        entity.setSummary(req.getSummary());
        entity.setContent(req.getContent());
        entity.setProjectId(req.getProjectId());
        entity.setSource(req.getSource());
    }

    private int nvl(Integer v) {
        return v == null ? 0 : v;
    }

    private void checkCategoryExists(Long categoryId) {
        if (categoryId == null) {
            return;
        }
        if (categoryMapper.selectById(categoryId) == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "资讯分类不存在");
        }
    }

    private void checkProjectExists(Long projectId) {
        if (projectId == null) {
            return;
        }
        IchProject project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "关联的非遗项目不存在");
        }
        if (project.getAuditStatus() == null
                || project.getAuditStatus() != StatusConstants.Audit.PUBLISHED) {
            throw new BizException(ResultCode.PARAM_ERROR, "关联的非遗项目尚未发布");
        }
    }
}

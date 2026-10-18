package com.yuxi.ich.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.project.dto.ProjectQueryParam;
import com.yuxi.ich.project.dto.ProjectSaveRequest;
import com.yuxi.ich.project.dto.RelatedInheritorView;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.project.mapper.CategoryCount;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.inheritor.entity.IchInheritor;
import com.yuxi.ich.inheritor.service.InheritorService;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.security.SecurityUtils;
import com.yuxi.ich.user.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 非遗项目服务。
 *
 * <p>本模块是其余业务模块的<b>参照实现</b>：分页查询、条件筛选、
 * 详情查询、热度自增、写操作的校验与异常处理都在这里给出范例。
 *
 * <p>Controller 只做参数接收与权限声明，业务规则全部下沉到本层；
 * 这样单元测试可以直接测 Service，不必启动 Web 容器。
 *
 * <p><b>数据级权限</b>：项目通过 {@link IchProject#getInheritorId()}（传承人档案 id）
 * 归属。管理员可维护任意项目；已认证传承人只能维护归属于自己的项目；
 * 新增项目时传承人的归属由后端按当前登录用户与其已认证档案强制确定，
 * 不信任前端提交的 inheritorId。
 */
@Service
public class ProjectService {

    private final IchProjectMapper projectMapper;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final InheritorService inheritorService;

    public ProjectService(IchProjectMapper projectMapper,
                           AuditLogService auditLogService,
                           UserService userService,
                           InheritorService inheritorService) {
        this.projectMapper = projectMapper;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.inheritorService = inheritorService;
    }

    /**
     * 前台分页查询：只看已发布内容。
     */
    public PageResult<IchProject> pageForFront(ProjectQueryParam query) {
        // 前台强制只看已发布，防止通过参数越权看到未审核内容
        query.setAuditStatus(1);
        return doPage(query);
    }

    /**
     * 后台分页查询：可查看全部状态。
     */
    public PageResult<IchProject> pageForAdmin(ProjectQueryParam query) {
        return doPage(query);
    }

    private PageResult<IchProject> doPage(ProjectQueryParam query) {
        long offset = (query.getPageNum() - 1) * query.getPageSize();
        long limit = query.getPageSize();
        List<IchProject> records = projectMapper.selectProjectPage(query, offset, limit);
        long total = projectMapper.countProject(query);
        return new PageResult<>(total, records, query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询详情。
     *
     * @param id          项目 id
     * @param onlyPublic  是否仅限已发布（前台传 true，后台传 false）
     * @param countView   是否累加浏览量（详情页传 true，编辑回显传 false）
     */
    public IchProject getDetail(Long id, boolean onlyPublic, boolean countView) {
        IchProject project = projectMapper.selectProjectDetail(id);
        if (project == null) {
            throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在或已下架");
        }
        if (onlyPublic && project.getAuditStatus() != null && project.getAuditStatus() != 1) {
            // 对外不暴露「存在但未发布」这一事实，统一按不存在处理
            throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在或已下架");
        }
        if (countView) {
            projectMapper.incrementViewCount(id);
            // 浏览历史（登录用户）
            userService.recordBrowse(StatusConstants.Interaction.TARGET_PROJECT, id);
        }
        // 只读装配关联传承人（来自 ich_project_inheritor，仅展示，不参与归属判定）
        List<RelatedInheritorView> related = projectMapper.selectRelatedInheritors(id);
        project.setRelatedInheritors(related == null ? List.of() : related);
        return project;
    }

    /** 热门项目 */
    public List<IchProject> hot(int limit) {
        int n = Math.min(Math.max(limit, 1), 20);
        return projectMapper.selectHotProjects(n);
    }

    /** 各类别项目数统计 */
    public List<CategoryCount> countByCategory() {
        return projectMapper.countByCategory();
    }

    /**
     * 新增项目。
     *
     * <p>新提交的内容一律进入待审核状态，前台不可见，
     * 由管理员审核通过后才发布。
     *
     * <p>归属确定：管理员可自行指定传承人档案；传承人创建时忽略前端传入的
     * inheritorId，强制绑定为当前登录用户自己的已认证档案（无档案则 403）。
     */
    public Long create(ProjectSaveRequest req) {
        checkProjectCodeUnique(req.getProjectCode(), null);
        IchProject entity = new IchProject();
        apply(req, entity);
        entity.setInheritorId(resolveInheritorIdForWrite(req.getInheritorId()));
        entity.setAuditStatus(0);
        entity.setViewCount(0);
        entity.setFavoriteCount(0);
        if (entity.getSortNo() == null) {
            entity.setSortNo(0);
        }
        projectMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 修改项目。
     *
     * <p>修改后重新进入待审核，避免绕过审核直接改已发布内容。
     *
     * <p>数据级权限：管理员可改任意项目；传承人只能改归属于自己的项目，
     * 且修改时不允许把归属改挂到其他传承人名下。
     */
    public void update(ProjectSaveRequest req) {
        if (req.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少项目 id");
        }
        IchProject exist = projectMapper.selectById(req.getId());
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在");
        }
        checkCanManage(exist.getInheritorId());
        checkProjectCodeUnique(req.getProjectCode(), req.getId());
        IchProject entity = new IchProject();
        entity.setId(req.getId());
        apply(req, entity);
        entity.setInheritorId(resolveInheritorIdForWrite(req.getInheritorId()));
        entity.setAuditStatus(0);
        projectMapper.updateById(entity);
    }

    /** 逻辑删除。仅管理员或归属传承人可执行。 */
    public void delete(Long id) {
        IchProject exist = projectMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在");
        }
        checkCanManage(exist.getInheritorId());
        // deleteById 会走逻辑删除（deleted = 1），数据仍保留可追溯
        projectMapper.deleteById(id);
    }

    /** 审核：通过 / 驳回 / 下架 */
    public void audit(Long id, Integer auditStatus, String remark) {
        if (auditStatus == null || auditStatus < 1 || auditStatus > 3) {
            throw new BizException(ResultCode.PARAM_ERROR, "审核状态不合法");
        }
        IchProject exist = projectMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在");
        }
        IchProject update = new IchProject();
        update.setId(id);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(remark);
        projectMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_PROJECT, id,
                auditStatus == 1 ? AuditLogService.ACTION_PASS
                        : auditStatus == 2 ? AuditLogService.ACTION_OFFLINE
                        : AuditLogService.ACTION_REJECT,
                remark);
    }

    /** 校验项目编号唯一 */
    private void checkProjectCodeUnique(String code, Long excludeId) {
        if (code == null || code.isBlank()) {
            return;
        }
        Long count = projectMapper.selectCount(
                Wrappers.<IchProject>lambdaQuery()
                        .eq(IchProject::getProjectCode, code)
                        .ne(excludeId != null, IchProject::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.CONFLICT, "项目编号已存在：" + code);
        }
    }

    // ==================== 数据级权限 ====================

    /**
     * 校验当前用户能否维护该资源。
     *
     * <p>管理员放行；传承人必须满足「资源归属自己的已认证档案」，
     * 否则统一抛 403，不泄露资源是否存在之外的信息。
     */
    private void checkCanManage(Long ownerInheritorId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long mine = ownCertifiedInheritorId();
        if (mine == null || ownerInheritorId == null || !mine.equals(ownerInheritorId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作该资源");
        }
    }

    /**
     * 写操作的归属绑定：管理员可指定传承人档案，传承人强制绑定本人。
     *
     * <p>不信任前端提交的 inheritorId——传承人即使伪造他人档案 id，
     * 也会被覆盖为当前登录用户自己的已认证档案 id。
     */
    private Long resolveInheritorIdForWrite(Long requestedInheritorId) {
        if (SecurityUtils.isAdmin()) {
            return requestedInheritorId;
        }
        Long mine = ownCertifiedInheritorId();
        if (mine == null) {
            throw new BizException(ResultCode.FORBIDDEN, "尚未通过传承人认证，无法发布内容");
        }
        return mine;
    }

    /** 当前登录用户已认证传承人档案 id；无档案返回 null */
    private Long ownCertifiedInheritorId() {
        IchInheritor profile =
                inheritorService.findCertifiedByUserId(SecurityUtils.getUserId());
        return profile == null ? null : profile.getId();
    }

    /** 把请求体字段拷贝到实体 */
    private void apply(ProjectSaveRequest req, IchProject entity) {
        entity.setProjectName(req.getProjectName());
        entity.setProjectCode(req.getProjectCode());
        entity.setCategoryId(req.getCategoryId());
        entity.setLevel(req.getLevel() == null ? 1 : req.getLevel());
        entity.setRegion(req.getRegion());
        entity.setBatchNo(req.getBatchNo());
        entity.setAnnounceYear(req.getAnnounceYear());
        entity.setType(req.getType());
        entity.setProtectionUnit(req.getProtectionUnit());
        entity.setCoverUrl(req.getCoverUrl());
        entity.setSummary(req.getSummary());
        entity.setContent(req.getContent());
        entity.setHistory(req.getHistory());
        entity.setStatusDesc(req.getStatusDesc());
        entity.setInheritorId(req.getInheritorId());
        entity.setSortNo(req.getSortNo());
    }
}

package com.yuxi.ich.inheritor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.constant.RoleConstants;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.inheritor.dto.InheritorApplyRequest;
import com.yuxi.ich.inheritor.dto.InheritorQueryParam;
import com.yuxi.ich.inheritor.dto.InheritorUpdateRequest;
import com.yuxi.ich.inheritor.entity.IchInheritor;
import com.yuxi.ich.inheritor.mapper.IchInheritorMapper;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.user.entity.SysRole;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.mapper.SysRoleMapper;
import com.yuxi.ich.user.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 非遗传承人服务。
 *
 * <p>相比项目模块，本模块多了一条<b>角色提升</b>链路：
 * 普通用户提交资质申请 → 管理员审核通过 → 该用户角色升级为 ROLE_INHERITOR。
 * 审核与角色变更必须在同一事务内完成，避免出现「认证通过但进不了工作台」
 * 或「有了传承人权限却没有传承人档案」的中间态。
 */
@Service
public class InheritorService {

    private static final Logger log = LoggerFactory.getLogger(InheritorService.class);

    private final IchInheritorMapper inheritorMapper;
    private final IchProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final AuditLogService auditLogService;

    public InheritorService(IchInheritorMapper inheritorMapper,
                            IchProjectMapper projectMapper,
                            SysUserMapper userMapper,
                            SysRoleMapper roleMapper,
                            AuditLogService auditLogService) {
        this.inheritorMapper = inheritorMapper;
        this.projectMapper = projectMapper;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.auditLogService = auditLogService;
    }

    // ==================== 查询 ====================

    /** 前台分页：只看已认证 */
    public PageResult<IchInheritor> pageForFront(InheritorQueryParam query) {
        query.setCertStatus(StatusConstants.CertStatus.CERTIFIED);
        return doPage(query);
    }

    /** 后台分页：可看全部状态 */
    public PageResult<IchInheritor> pageForAdmin(InheritorQueryParam query) {
        return doPage(query);
    }

    private PageResult<IchInheritor> doPage(InheritorQueryParam query) {
        long offset = (query.getPageNum() - 1) * query.getPageSize();
        List<IchInheritor> records =
                inheritorMapper.selectInheritorPage(query, offset, query.getPageSize());
        long total = inheritorMapper.countInheritor(query);
        return new PageResult<>(total, records, query.getPageNum(), query.getPageSize());
    }

    /**
     * 详情。
     *
     * @param onlyCertified 前台传 true，只有已认证的传承人才对外可见
     * @param countView     是否累加主页访问量
     */
    public IchInheritor getDetail(Long id, boolean onlyCertified, boolean countView) {
        IchInheritor inheritor = inheritorMapper.selectInheritorDetail(id);
        if (inheritor == null) {
            throw new BizException(ResultCode.NOT_FOUND, "传承人不存在");
        }
        if (onlyCertified && inheritor.getCertStatus() != null
                && inheritor.getCertStatus() != StatusConstants.CertStatus.CERTIFIED) {
            throw new BizException(ResultCode.NOT_FOUND, "传承人不存在");
        }
        if (countView) {
            inheritorMapper.incrementViewCount(id);
        }
        return inheritor;
    }

    /** 查询当前登录用户自己的传承人档案 */
    public IchInheritor getMyProfile(Long userId) {
        IchInheritor inheritor = findByUserId(userId);
        if (inheritor == null) {
            throw new BizException(ResultCode.NOT_FOUND, "尚未提交传承人资质申请");
        }
        return inheritor;
    }

    /**
     * 查询某账号名下<b>已认证</b>的传承人档案。
     *
     * <p>供项目/活动模块做数据级权限校验：
     * 当前登录用户 → 找到自己的已认证档案 → 得到档案 id，
     * 再与资源（项目/活动）的 inheritorId 比对，判断是否归属本人。
     *
     * @param userId 账号 id
     * @return 已认证档案；未申请、待审核、已驳回或账号不存在均返回 null
     */
    public IchInheritor findCertifiedByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        IchInheritor inheritor = findByUserId(userId);
        if (inheritor == null || inheritor.getCertStatus() == null
                || inheritor.getCertStatus() != StatusConstants.CertStatus.CERTIFIED) {
            return null;
        }
        return inheritor;
    }

    // ==================== 申请与审核 ====================

    /**
     * 提交资质申请。
     *
     * <p>同一账号只允许存在一条档案：
     * <ul>
     *   <li>从未申请过 → 新建，状态为待审核</li>
     *   <li>曾被驳回 → 允许修改后重新提交，状态回到待审核</li>
     *   <li>已认证或待审核 → 拒绝，避免重复提交与审核冲突</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    public Long apply(Long userId, InheritorApplyRequest req) {
        IchInheritor exist = findByUserId(userId);

        if (exist != null) {
            Integer status = exist.getCertStatus();
            if (status != null && status == StatusConstants.CertStatus.CERTIFIED) {
                throw new BizException(ResultCode.CONFLICT, "您已是认证传承人，无需重复申请");
            }
            if (status != null && status == StatusConstants.CertStatus.PENDING) {
                throw new BizException(ResultCode.CONFLICT, "申请正在审核中，请耐心等待");
            }
            // 已驳回：允许修改后重新提交
            applyTo(exist, req);
            exist.setCertStatus(StatusConstants.CertStatus.PENDING);
            inheritorMapper.updateById(exist);
            log.info("传承人资质重新提交：userId={} inheritorId={}", userId, exist.getId());
            return exist.getId();
        }

        checkCertNoUnique(req.getCertNo(), null);
        checkProjectExists(req.getProjectId());

        IchInheritor entity = new IchInheritor();
        entity.setUserId(userId);
        applyTo(entity, req);
        entity.setCertStatus(StatusConstants.CertStatus.PENDING);
        entity.setViewCount(0);
        inheritorMapper.insert(entity);
        log.info("传承人资质首次提交：userId={} inheritorId={}", userId, entity.getId());
        return entity.getId();
    }

    /**
     * 认证审核。
     *
     * <p>通过时在同一事务内把用户角色提升为 ROLE_INHERITOR，
     * 保证「档案已认证」与「账号有传承人权限」两件事原子生效。
     *
     * @param certStatus 1 通过，2 驳回
     */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Integer certStatus) {
        if (certStatus == null
                || (certStatus != StatusConstants.CertStatus.CERTIFIED
                && certStatus != StatusConstants.CertStatus.REJECTED)) {
            throw new BizException(ResultCode.PARAM_ERROR, "审核结果只能是 1（通过）或 2（驳回）");
        }
        IchInheritor inheritor = inheritorMapper.selectById(id);
        if (inheritor == null) {
            throw new BizException(ResultCode.NOT_FOUND, "传承人档案不存在");
        }

        IchInheritor update = new IchInheritor();
        update.setId(id);
        update.setCertStatus(certStatus);
        inheritorMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_INHERITOR, id,
                certStatus == StatusConstants.CertStatus.CERTIFIED
                        ? AuditLogService.ACTION_PASS : AuditLogService.ACTION_REJECT,
                null);

        if (certStatus == StatusConstants.CertStatus.CERTIFIED) {
            promoteToInheritor(inheritor.getUserId(), id);
        }
    }

    /**
     * 把档案关联的账号提升为传承人角色。
     *
     * <p>历史传承人没有关联账号（userId 为空），跳过；
     * 若账号已是管理员则不降级也不提升，保持管理员权限。
     */
    private void promoteToInheritor(Long userId, Long inheritorId) {
        if (userId == null) {
            log.info("传承人档案 {} 无关联账号（历史名录数据），跳过角色提升", inheritorId);
            return;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("传承人档案 {} 关联的账号 {} 不存在，跳过角色提升", inheritorId, userId);
            return;
        }
        SysRole currentRole = user.getRoleId() == null ? null
                : roleMapper.selectById(user.getRoleId());
        if (currentRole != null && RoleConstants.ADMIN.equals(currentRole.getRoleCode())) {
            log.info("账号 {} 当前为管理员，保持角色不变", userId);
            return;
        }
        SysRole inheritorRole = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, RoleConstants.INHERITOR));
        if (inheritorRole == null) {
            throw new BizException(ResultCode.ERROR, "角色未初始化：ROLE_INHERITOR");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setRoleId(inheritorRole.getId());
        userMapper.updateById(update);
        log.info("账号 {} 角色已提升为 ROLE_INHERITOR（因档案 {} 认证通过）", userId, inheritorId);
    }

    // ==================== 维护 ====================

    /**
     * 传承人维护自己的档案。
     *
     * <p>数据级权限：只能改自己关联的那条档案，防止篡改 id 改他人资料。
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMyProfile(Long userId, InheritorUpdateRequest req) {
        IchInheritor mine = findByUserId(userId);
        if (mine == null) {
            throw new BizException(ResultCode.NOT_FOUND, "尚未提交传承人资质申请");
        }
        if (mine.getCertStatus() == null
                || mine.getCertStatus() != StatusConstants.CertStatus.CERTIFIED) {
            throw new BizException(ResultCode.FORBIDDEN, "认证通过后才能维护个人主页");
        }
        IchInheritor update = new IchInheritor();
        update.setId(mine.getId());
        update.setRegion(req.getRegion());
        update.setCraftYears(req.getCraftYears());
        update.setIntro(req.getIntro());
        update.setWorks(req.getWorks());
        update.setContact(req.getContact());
        update.setCertImg(req.getCertImg());
        inheritorMapper.updateById(update);
    }

    /** 管理员维护指定档案 */
    @Transactional(rollbackFor = Exception.class)
    public void updateByAdmin(Long id, InheritorUpdateRequest req) {
        IchInheritor exist = inheritorMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "传承人档案不存在");
        }
        IchInheritor update = new IchInheritor();
        update.setId(id);
        update.setRegion(req.getRegion());
        update.setCraftYears(req.getCraftYears());
        update.setIntro(req.getIntro());
        update.setWorks(req.getWorks());
        update.setContact(req.getContact());
        update.setCertImg(req.getCertImg());
        inheritorMapper.updateById(update);
    }

    /** 逻辑删除 */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (inheritorMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "传承人档案不存在");
        }
        inheritorMapper.deleteById(id);
    }

    // ==================== 内部工具 ====================

    /**
     * 按关联账号查询传承人档案。
     *
     * <p>用 Wrapper 而非自定义 XML：一是省去接口与 XML 两处维护，
     * 二是 Wrapper 查询会自动带上逻辑删除条件（deleted = 0）。
     * 同一账号只允许一条档案，业务侧已通过 apply 流程保证。
     */
    private IchInheritor findByUserId(Long userId) {
        return inheritorMapper.selectOne(
                Wrappers.<IchInheritor>lambdaQuery()
                        .eq(IchInheritor::getUserId, userId)
                        .orderByDesc(IchInheritor::getId)
                        .last("LIMIT 1"));
    }
    // ==================== 内部工具 ====================

    private void applyTo(IchInheritor entity, InheritorApplyRequest req) {
        entity.setName(req.getName());
        entity.setGender(req.getGender());
        entity.setLevel(req.getLevel() == null ? StatusConstants.Level.NATIONAL : req.getLevel());
        entity.setRegion(req.getRegion());
        entity.setProjectId(req.getProjectId());
        entity.setCraftYears(req.getCraftYears());
        entity.setCertNo(req.getCertNo());
        entity.setCertImg(req.getCertImg());
        entity.setIntro(req.getIntro());
        entity.setWorks(req.getWorks());
        entity.setContact(req.getContact());
    }

    /** 认定编号唯一性校验 */
    private void checkCertNoUnique(String certNo, Long excludeId) {
        if (certNo == null || certNo.isBlank()) {
            return;
        }
        Long count = inheritorMapper.selectCount(
                Wrappers.<IchInheritor>lambdaQuery()
                        .eq(IchInheritor::getCertNo, certNo)
                        .ne(excludeId != null, IchInheritor::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.CONFLICT, "认定编号已存在：" + certNo);
        }
    }

    /** 校验关联的非遗项目存在且已发布 */
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
            throw new BizException(ResultCode.PARAM_ERROR, "关联的非遗项目尚未发布，无法选择");
        }
    }
}

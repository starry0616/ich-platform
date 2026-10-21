package com.yuxi.ich.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.activity.dto.ActivityQueryParam;
import com.yuxi.ich.activity.dto.ActivitySaveRequest;
import com.yuxi.ich.activity.entity.ActSignup;
import com.yuxi.ich.activity.entity.IchActivity;
import com.yuxi.ich.activity.mapper.ActSignupMapper;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.inheritor.entity.IchInheritor;
import com.yuxi.ich.inheritor.service.InheritorService;
import com.yuxi.ich.interaction.service.InteractionService;
import com.yuxi.ich.user.service.UserService;
import com.yuxi.ich.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 非遗活动与报名服务。
 *
 * <p><b>本模块是系统里唯一存在并发写入竞争的模块</b>：多个用户同时报名同一活动时
 * 都要扣减同一份名额。防超卖采用三重保障，缺一不可：
 *
 * <ol>
 *   <li><b>数据库唯一约束</b> (activity_id, user_id) —— 从数据层杜绝同一用户重复报名</li>
 *   <li><b>条件更新</b> {@code UPDATE ... WHERE signed < quota} —— 数据库层面保证名额不会被扣成负数</li>
 *   <li><b>乐观锁</b> version 字段 —— 并发事务中只有一个能成功，其余失败后重试</li>
 * </ol>
 *
 * <p>为什么不用悲观锁（SELECT ... FOR UPDATE）：本场景读多写少、冲突概率低，
 * 乐观锁在无冲突时没有加锁开销，吞吐更好；冲突时重试几次即可。
 * 若活动报名极端火爆（如秒杀），则应改为 Redis 预扣 + 消息队列削峰。
 */
@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    /** 名额扣减的最大重试次数 */
    private static final int MAX_RETRY = 3;

    /** 报名编号中的日期部分格式 */
    private static final DateTimeFormatter NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final IchActivityMapper activityMapper;
    private final ActSignupMapper signupMapper;
    private final InteractionService interactionService;
    private final AuditLogService auditLogService;
    private final UserService userService;
    private final InheritorService inheritorService;

    public ActivityService(IchActivityMapper activityMapper,
                           ActSignupMapper signupMapper,
                           InteractionService interactionService,
                           AuditLogService auditLogService,
                           UserService userService,
                           InheritorService inheritorService) {
        this.activityMapper = activityMapper;
        this.signupMapper = signupMapper;
        this.interactionService = interactionService;
        this.auditLogService = auditLogService;
        this.userService = userService;
        this.inheritorService = inheritorService;
    }

    // ==================== 查询 ====================

    /** 前台分页：只看已通过审核的活动 */
    public PageResult<IchActivity> pageForFront(ActivityQueryParam query) {
        query.setOnlyPublished(true);
        return doPage(query);
    }

    /** 后台分页：可看待审核等全部状态 */
    public PageResult<IchActivity> pageForAdmin(ActivityQueryParam query) {
        query.setOnlyPublished(false);
        return doPage(query);
    }

    private PageResult<IchActivity> doPage(ActivityQueryParam query) {
        long offset = (query.getPageNum() - 1) * query.getPageSize();
        List<IchActivity> records =
                activityMapper.selectActivityPage(query, offset, query.getPageSize());
        long total = activityMapper.countActivity(query);
        return new PageResult<>(total, records, query.getPageNum(), query.getPageSize());
    }

    /**
     * 活动详情。
     *
     * @param onlyPublished 前台传 true
     */
    public IchActivity getDetail(Long id, boolean onlyPublished) {
        IchActivity activity = activityMapper.selectActivityDetail(id);
        if (activity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }
        // 公开端仅放行报名中(status=1)：待审核0、驳回/取消5均按不存在处理，
        // 防止普通用户凭活动 id 直接访问不可参与活动
        if (onlyPublished && !Integer.valueOf(1).equals(activity.getStatus())) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在或未发布");
        }
        if (activity.getRemaining() == null) {
            activity.setRemaining(Math.max(activity.getQuota() - activity.getSigned(), 0));
        }
        if (onlyPublished) {
            // 浏览历史（登录用户）
            userService.recordBrowse(StatusConstants.Interaction.TARGET_ACTIVITY, id);
        }
        return activity;
    }

    // ==================== 报名 ====================

    /**
     * 活动报名。
     *
     * <p>流程：前置校验 → 扣减名额（乐观锁重试）→ 写入报名记录 → 生成报名编号。
     *
     * <p>事务边界说明：整段放在一个事务里。若名额扣减成功但报名记录写入失败
     * （如唯一约束冲突），事务回滚会把名额也一并还原，不会出现「名额被占但没人报名」。
     */
    @Transactional(rollbackFor = Exception.class)
    public ActSignup signup(Long activityId, String realName, String phone, String remark) {
        Long userId = SecurityUtils.getUserId();

        IchActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }

        // ---------- 前置校验 ----------
        if (activity.getStatus() == null || activity.getStatus() != 1) {
            throw new BizException(ResultCode.CONFLICT, "该活动当前不可报名");
        }
        if (activity.getSignupDeadline() != null
                && activity.getSignupDeadline().isBefore(LocalDateTime.now())) {
            throw new BizException(ResultCode.CONFLICT, "报名已截止");
        }
        // 重复报名检查：
        // 只查「已报名/已核销」的生效记录；已取消（status=2）不算重复，
        // 但下面会复用该行而不是新插，以规避 (activity_id, user_id) 唯一约束。
        ActSignup active = signupMapper.selectOne(Wrappers.<ActSignup>lambdaQuery()
                .eq(ActSignup::getActivityId, activityId)
                .eq(ActSignup::getUserId, userId)
                .in(ActSignup::getStatus, 1, 3));
        if (active != null) {
            throw new BizException(ResultCode.CONFLICT, "您已报名该活动，请勿重复报名");
        }
        // 查已被取消的记录，用于复用（唯一约束 (activity_id, user_id) 不含 deleted，
        // 因此不能简单地再 INSERT 一条）
        ActSignup canceled = signupMapper.selectOne(Wrappers.<ActSignup>lambdaQuery()
                .eq(ActSignup::getActivityId, activityId)
                .eq(ActSignup::getUserId, userId)
                .eq(ActSignup::getStatus, 2)
                .orderByDesc(ActSignup::getId)
                .last("LIMIT 1"));

        if (activity.getSigned() != null && activity.getQuota() != null
                && activity.getSigned() >= activity.getQuota()) {
            throw new BizException(ResultCode.CONFLICT, "活动名额已满");
        }

        // ---------- 扣减名额（乐观锁 + 条件更新，失败重试） ----------
        boolean occupied = false;
        for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
            IchActivity fresh = activityMapper.selectById(activityId);
            if (fresh == null) {
                throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
            }
            int rows = activityMapper.tryOccupyQuota(activityId, fresh.getVersion());
            if (rows > 0) {
                occupied = true;
                log.debug("名额扣减成功 activity={} user={} 第 {} 次尝试", activityId, userId, attempt);
                break;
            }
            log.debug("名额扣减冲突 activity={} user={} 第 {} 次尝试失败，准备重试",
                    activityId, userId, attempt);
        }
        if (!occupied) {
            // 重试仍失败：要么名额已满，要么并发过于激烈
            Integer remaining = activityMapper.selectRemaining(activityId);
            if (remaining != null && remaining <= 0) {
                throw new BizException(ResultCode.CONFLICT, "活动名额已满");
            }
            throw new BizException(ResultCode.CONFLICT, "报名人数较多，请稍后重试");
        }

        // ---------- 写入报名记录 ----------
        ActSignup signup;
        if (canceled != null) {
            // 复用被取消的那条记录：恢复状态并更新快照信息，
            // 这样既保留报名历史，也不会撞 (activity_id, user_id) 唯一约束
            signup = canceled;
            signup.setSignupNo(generateSignupNo(activityId, userId));
            signup.setRealName(resolveRealName(realName));
            signup.setPhone(phone);
            signup.setRemark(remark);
            signup.setSignupTime(LocalDateTime.now());
            signup.setCancelTime(null);
            signup.setStatus(1);
            signup.setCheckinStatus(0);
            signup.setCheckinTime(null);
            signup.setCheckinBy(null);
            signupMapper.updateById(signup);
            log.info("复用已取消的报名记录重新报名 activity={} user={} signupId={}",
                    activityId, userId, signup.getId());
        } else {
            signup = new ActSignup();
            signup.setActivityId(activityId);
            signup.setUserId(userId);
            signup.setSignupNo(generateSignupNo(activityId, userId));
            signup.setRealName(resolveRealName(realName));
            signup.setPhone(phone);
            signup.setRemark(remark);
            signup.setSignupTime(LocalDateTime.now());
            signup.setStatus(1);
            signup.setCheckinStatus(0);
            try {
                signupMapper.insert(signup);
            } catch (DuplicateKeyException e) {
                // 唯一约束兜底：极端并发下两个请求都通过了前置校验
                throw new BizException(ResultCode.CONFLICT, "您已报名该活动，请勿重复报名");
            }
        }

        log.info("报名成功 activity={} user={} signupNo={}", activityId, userId, signup.getSignupNo());
        return signup;
    }

    private String resolveRealName(String realName) {
        if (realName != null && !realName.isBlank()) {
            return realName;
        }
        return SecurityUtils.getLoginUser().getNickname();
    }

    /**
     * 取消报名。
     *
     * <p>边界规则（与说明书 4.5 节一致）：
     * 报名截止前可取消；活动开始前 24 小时内不允许取消，以保障活动组织。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelSignup(Long activityId) {
        Long userId = SecurityUtils.getUserId();
        IchActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }
        ActSignup signup = signupMapper.selectOne(Wrappers.<ActSignup>lambdaQuery()
                .eq(ActSignup::getActivityId, activityId)
                .eq(ActSignup::getUserId, userId)
                .eq(ActSignup::getStatus, 1));
        if (signup == null) {
            throw new BizException(ResultCode.NOT_FOUND, "未找到有效报名记录");
        }
        if (signup.getCheckinStatus() != null && signup.getCheckinStatus() == 1) {
            throw new BizException(ResultCode.CONFLICT, "已核销的报名无法取消");
        }
        // 活动开始前 24 小时内不允许取消
        if (activity.getStartTime() != null
                && activity.getStartTime().isBefore(LocalDateTime.now().plusHours(24))) {
            throw new BizException(ResultCode.CONFLICT, "活动开始前 24 小时内不可取消报名");
        }

        ActSignup update = new ActSignup();
        update.setId(signup.getId());
        update.setStatus(2);
        update.setCancelTime(LocalDateTime.now());
        // 注意：这里是「标记取消」，不是物理删除。
        // 保留记录便于活动主办方统计取消率，也避免再次报名时撞 (activity_id, user_id) 唯一约束
        // （该唯一约束不含 deleted 列，逻辑删除后重新插入仍会冲突，因此改为复用同一行）。
        signupMapper.updateById(update);

        activityMapper.releaseQuota(activityId);
        log.info("取消报名 activity={} user={}", activityId, userId);
    }

    /**
     * 现场核销（主办传承人或管理员）。
     *
     * <p>数据级权限：管理员可核销任意活动；传承人必须确为该活动的主办传承人，
     * 否则 403，防止越权核销并泄露报名信息。
     *
     * @param signupNo 报名编号；也支持传报名记录 id
     */
    @Transactional(rollbackFor = Exception.class)
    public ActSignup checkin(Long activityId, String signupNo) {
        checkActivityHost(activityId);
        ActSignup signup = signupMapper.selectOne(Wrappers.<ActSignup>lambdaQuery()
                .eq(ActSignup::getActivityId, activityId)
                .eq(ActSignup::getSignupNo, signupNo));
        if (signup == null) {
            throw new BizException(ResultCode.NOT_FOUND, "报名记录不存在");
        }
        if (signup.getStatus() != null && signup.getStatus() == 2) {
            throw new BizException(ResultCode.CONFLICT, "该报名已取消，无法核销");
        }
        if (signup.getCheckinStatus() != null && signup.getCheckinStatus() == 1) {
            throw new BizException(ResultCode.CONFLICT, "该报名已核销，请勿重复操作");
        }

        ActSignup update = new ActSignup();
        update.setId(signup.getId());
        update.setCheckinStatus(1);
        update.setCheckinTime(LocalDateTime.now());
        update.setCheckinBy(SecurityUtils.getUserId());
        update.setStatus(3);
        signupMapper.updateById(update);

        log.info("核销成功 activity={} signupNo={}", activityId, signupNo);
        return signupMapper.selectById(signup.getId());
    }

    /**
     * 某活动的报名名单（含姓名/电话等敏感信息）。
     *
     * <p>数据级权限：管理员可查看任意活动名单；传承人只能查看自己主办的活动，
     * 否则 403，防止越权查看他人活动报名名单。
     */
    public PageResult<ActSignup> signupList(Long activityId, Integer status,
                                            long pageNum, long pageSize) {
        checkActivityHost(activityId);
        long offset = (pageNum - 1) * pageSize;
        List<ActSignup> records =
                signupMapper.selectByActivity(activityId, status, offset, pageSize);
        long total = signupMapper.countByActivity(activityId, status);
        return new PageResult<>(total, records, pageNum, pageSize);
    }

    /** 我的报名列表 */
    public PageResult<ActSignup> mySignups(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long offset = (pageNum - 1) * pageSize;
        List<ActSignup> records = signupMapper.selectMySignups(userId, offset, pageSize);
        long total = signupMapper.countMySignups(userId);
        return new PageResult<>(total, records, pageNum, pageSize);
    }

    // ==================== 活动维护 ====================

    /**
     * 发布活动。
     *
     * <p>提交后进入待审核（status = 0）；管理员审核通过后置为报名中（status = 1）。
     *
     * <p>归属确定：管理员可自行指定主办传承人档案；传承人创建时忽略前端传入的
     * inheritorId，强制绑定为当前登录用户自己的已认证档案（无档案则 403）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(ActivitySaveRequest req) {
        validateTimeRange(req);
        IchActivity entity = new IchActivity();
        apply(req, entity);
        entity.setInheritorId(resolveInheritorIdForWrite(req.getInheritorId()));
        entity.setSigned(0);
        entity.setVersion(0);
        entity.setStatus(0);
        activityMapper.insert(entity);
        log.info("活动提交：id={} title={}", entity.getId(), entity.getTitle());
        return entity.getId();
    }

    /**
     * 修改活动。改后重新进入待审核。
     *
     * <p>数据级权限：管理员可改任意活动；传承人只能改自己主办的活动，
     * 且修改时不允许把主办归属改挂到其他传承人名下。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(ActivitySaveRequest req) {
        if (req.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少活动 id");
        }
        IchActivity exist = activityMapper.selectById(req.getId());
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }
        checkActivityHost(req.getId());
        validateTimeRange(req);

        // 名额不允许改到小于已报名人数，否则会破坏 signed <= quota 的约束
        if (req.getQuota() != null && exist.getSigned() != null
                && req.getQuota() < exist.getSigned()) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "名额上限不能小于已报名人数（" + exist.getSigned() + "）");
        }

        IchActivity update = new IchActivity();
        update.setId(req.getId());
        apply(req, update);
        update.setInheritorId(resolveInheritorIdForWrite(req.getInheritorId()));
        update.setStatus(0);
        // 乐观锁字段需一并带上，避免并发修改覆盖
        update.setVersion(exist.getVersion());
        activityMapper.updateById(update);
    }

    /**
     * 逻辑删除，并级联清理该活动的互动记录。
     * 仅管理员或主办传承人可执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (activityMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }
        checkActivityHost(id);
        activityMapper.deleteById(id);
        // 多态关联无法靠外键级联，需显式清理
        interactionService.clearByTarget(StatusConstants.Interaction.TARGET_ACTIVITY, id);
    }

    /** 审核：通过（1 报名中）/ 驳回（5 已取消） */
    @Transactional(rollbackFor = Exception.class)
    public void audit(Long id, Integer status, String remark) {
        if (status == null || (status != 1 && status != 5)) {
            throw new BizException(ResultCode.PARAM_ERROR, "审核结果只能是 1（通过）或 5（驳回）");
        }
        IchActivity exist = activityMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
        }
        IchActivity update = new IchActivity();
        update.setId(id);
        update.setStatus(status);
        update.setAuditRemark(remark);
        update.setVersion(exist.getVersion());
        activityMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_ACTIVITY, id,
                status == 1 ? AuditLogService.ACTION_PASS : AuditLogService.ACTION_REJECT,
                remark);
    }

    // ==================== 内部工具 ====================

    private void validateTimeRange(ActivitySaveRequest req) {
        if (req.getStartTime() != null && req.getEndTime() != null
                && !req.getEndTime().isAfter(req.getStartTime())) {
            throw new BizException(ResultCode.PARAM_ERROR, "结束时间必须晚于开始时间");
        }
        if (req.getSignupDeadline() != null && req.getStartTime() != null
                && req.getSignupDeadline().isAfter(req.getStartTime())) {
            throw new BizException(ResultCode.PARAM_ERROR, "报名截止时间不能晚于活动开始时间");
        }
    }

    private void apply(ActivitySaveRequest req, IchActivity entity) {
        entity.setTitle(req.getTitle());
        entity.setActivityType(req.getActivityType());
        entity.setCoverUrl(req.getCoverUrl());
        entity.setProjectId(req.getProjectId());
        entity.setInheritorId(req.getInheritorId());
        entity.setContent(req.getContent());
        entity.setStartTime(req.getStartTime());
        entity.setEndTime(req.getEndTime());
        entity.setAddress(req.getAddress());
        entity.setLongitude(req.getLongitude());
        entity.setLatitude(req.getLatitude());
        entity.setQuota(req.getQuota());
        // 未填报名截止时间则默认取活动开始时间
        entity.setSignupDeadline(req.getSignupDeadline() != null
                ? req.getSignupDeadline() : req.getStartTime());
    }

    // ==================== 数据级权限 ====================

    /**
     * 校验当前用户能否查看/维护某活动（主办名单、核销、修改、删除）。
     *
     * <p>管理员放行；传承人必须满足「活动归属自己的已认证档案」，
     * 否则统一抛 403，不泄露活动是否存在之外的信息。
     */
    private void checkActivityHost(Long activityId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        IchActivity activity = activityMapper.selectById(activityId);
        Long ownerInheritorId = activity == null ? null : activity.getInheritorId();
        Long mine = ownCertifiedInheritorId();
        if (mine == null || ownerInheritorId == null || !mine.equals(ownerInheritorId)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作该资源");
        }
    }

    /**
     * 写操作的归属绑定：管理员可指定主办传承人档案，传承人强制绑定本人。
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

    /**
     * 生成报名编号，形如 YX20261004173012-3-0007。
     *
     * <p>含时间、活动 id、用户 id 与随机后缀，保证唯一且便于人工核对。
     * 数据库上另有唯一约束兜底。
     */
    private String generateSignupNo(Long activityId, Long userId) {
        return "YX" + LocalDateTime.now().format(NO_FORMAT)
                + "-" + activityId + "-" + userId
                + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}

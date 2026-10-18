package com.yuxi.ich.interaction.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.activity.entity.IchActivity;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.article.entity.CmsArticle;
import com.yuxi.ich.article.mapper.CmsArticleMapper;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.interaction.dto.InteractionRequest;
import com.yuxi.ich.interaction.entity.Interaction;
import com.yuxi.ich.interaction.mapper.InteractionMapper;
import com.yuxi.ich.message.service.MessageService;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户互动服务：收藏、点赞、评论。
 *
 * <p><b>幂等设计</b>（这是本模块的核心）：
 * 收藏与点赞是「开关式」操作，用户重复点击不应产生多条记录、也不应重复计数。
 * 实现方式为「先查后写」：
 * <pre>
 *   已存在有效记录 → 逻辑删除（取消），计数 -1
 *   不存在有效记录 → 新增，计数 +1
 * </pre>
 * 为什么不直接靠唯一索引兜底：唯一索引包含已逻辑删除的行，
 * 取消后再收藏会撞唯一键。因此业务层必须先查，再决定新增还是复活。
 *
 * <p><b>多态关联的一致性补偿</b>：interaction 表用 target_type + target_id
 * 关联三类对象，无法用外键约束。因此这里做两件事：
 * 一是写入前校验目标确实存在且已发布；二是提供 {@link #clearByTarget}
 * 供项目/资讯/活动删除时级联清理。
 */
@Service
public class InteractionService {

    private static final Logger log = LoggerFactory.getLogger(InteractionService.class);

    /** 评论内容长度上限 */
    private static final int COMMENT_MAX_LEN = 500;

    private final InteractionMapper interactionMapper;
    private final IchProjectMapper projectMapper;
    private final CmsArticleMapper articleMapper;
    private final IchActivityMapper activityMapper;
    private final MessageService messageService;

    public InteractionService(InteractionMapper interactionMapper,
                              IchProjectMapper projectMapper,
                              CmsArticleMapper articleMapper,
                              IchActivityMapper activityMapper,
                              MessageService messageService) {
        this.interactionMapper = interactionMapper;
        this.projectMapper = projectMapper;
        this.articleMapper = articleMapper;
        this.activityMapper = activityMapper;
        this.messageService = messageService;
    }

    // ==================== 收藏 / 点赞 ====================

    /**
     * 切换收藏状态。
     *
     * @return true 表示本次操作后为「已收藏」，false 表示「已取消」
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleFavorite(Integer targetType, Long targetId) {
        return toggle(targetType, targetId, StatusConstants.Interaction.TYPE_FAVORITE);
    }

    /**
     * 切换点赞状态。
     *
     * @return true 表示本次操作后为「已点赞」
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleLike(Integer targetType, Long targetId) {
        return toggle(targetType, targetId, StatusConstants.Interaction.TYPE_LIKE);
    }

    private boolean toggle(Integer targetType, Long targetId, int type) {
        Long userId = SecurityUtils.getUserId();
        checkTargetType(targetType);
        checkTargetExists(targetType, targetId, true);

        int exists = interactionMapper.existsInteraction(userId, targetType, targetId, type);
        if (exists > 0) {
            // 取消：逻辑删除并回退计数
            interactionMapper.delete(Wrappers.<Interaction>lambdaQuery()
                    .eq(Interaction::getUserId, userId)
                    .eq(Interaction::getTargetType, targetType)
                    .eq(Interaction::getTargetId, targetId)
                    .eq(Interaction::getType, type));
            changeCounter(targetType, targetId, type, -1);
            log.debug("取消互动 user={} target={}-{} type={}", userId, targetType, targetId, type);
            return false;
        }

        // 新增前先物理清掉历史逻辑删除行。
        // 唯一约束 (user_id, target_type, target_id, type) 不含 deleted 列，
        // 若不清掉，「取消收藏后再次收藏」会撞唯一键报 Duplicate entry。
        interactionMapper.purgeDeleted(userId, targetType, targetId, type);

        Interaction entity = new Interaction();
        entity.setUserId(userId);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setType(type);
        entity.setParentId(0L);
        entity.setLikeCount(0);
        entity.setStatus(1);
        interactionMapper.insert(entity);
        changeCounter(targetType, targetId, type, 1);
        log.debug("新增互动 user={} target={}-{} type={}", userId, targetType, targetId, type);
        return true;
    }

    /**
     * 维护目标对象上的计数字段。
     *
     * <p>用 SQL 原地增减避免并发丢失更新。目前只有项目表维护 favorite_count，
     * 资讯与活动暂不维护（按需再扩展）。
     */
    private void changeCounter(Integer targetType, Long targetId, int type, int delta) {
        if (type != StatusConstants.Interaction.TYPE_FAVORITE) {
            return;
        }
        if (targetType == StatusConstants.Interaction.TARGET_PROJECT) {
            projectMapper.update(null, Wrappers.<IchProject>lambdaUpdate()
                    .setSql("favorite_count = GREATEST(favorite_count + (" + delta + "), 0)")
                    .eq(IchProject::getId, targetId));
        } else if (targetType == StatusConstants.Interaction.TARGET_ARTICLE) {
            // cms_article 暂未设计收藏计数字段，如需统计可在表中补列
            log.debug("资讯收藏计数暂未维护：articleId={}", targetId);
        }
    }

    /** 查询当前用户对某目标的互动状态，供前端按钮回显 */
    public boolean hasInteracted(Integer targetType, Long targetId, Integer type) {
        checkTargetType(targetType);
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return false;
        }
        return interactionMapper.existsInteraction(userId, targetType, targetId, type) > 0;
    }

    // ==================== 评论 ====================

    /**
     * 发表评论。
     *
     * <p>回复评论时校验父评论存在且属于同一目标对象，
     * 防止跨对象挂载造成数据混乱。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long comment(InteractionRequest req) {
        Long userId = SecurityUtils.getUserId();
        checkTargetType(req.getTargetType());
        checkTargetExists(req.getTargetType(), req.getTargetId(), true);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        if (content.isEmpty()) {
            throw new BizException(ResultCode.PARAM_ERROR, "评论内容不能为空");
        }
        if (content.length() > COMMENT_MAX_LEN) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "评论内容不能超过 " + COMMENT_MAX_LEN + " 字");
        }

        Long parentId = req.getParentId() == null ? 0L : req.getParentId();
        Interaction parent = null;
        if (parentId > 0) {
            parent = interactionMapper.selectById(parentId);
            if (parent == null || parent.getType() != StatusConstants.Interaction.TYPE_COMMENT) {
                throw new BizException(ResultCode.PARAM_ERROR, "被回复的评论不存在");
            }
            if (!parent.getTargetType().equals(req.getTargetType())
                    || !parent.getTargetId().equals(req.getTargetId())) {
                throw new BizException(ResultCode.PARAM_ERROR, "被回复的评论不属于当前内容");
            }
        }

        Interaction entity = new Interaction();
        entity.setUserId(userId);
        entity.setTargetType(req.getTargetType());
        entity.setTargetId(req.getTargetId());
        entity.setType(StatusConstants.Interaction.TYPE_COMMENT);
        entity.setContent(content);
        entity.setParentId(parentId);
        entity.setLikeCount(0);
        entity.setStatus(1);
        interactionMapper.insert(entity);

        // 互动通知（说明书 4.6 节）：回复他人评论时通知被回复人
        if (parent != null && parent.getUserId() != null && !parent.getUserId().equals(userId)) {
            messageService.send(parent.getUserId(), MessageService.TYPE_INTERACTION,
                    "收到新回复",
                    targetNameOf(req.getTargetType(), req.getTargetId())
                            + " 下有用户回复了你的评论：" + truncate(content, 40),
                    entity.getId());
        }
        return entity.getId();
    }

    /** 目标对象的展示名（通知文案用） */
    private String targetNameOf(Integer targetType, Long targetId) {
        return switch (targetType) {
            case StatusConstants.Interaction.TARGET_PROJECT -> {
                IchProject p = projectMapper.selectById(targetId);
                yield p == null ? "非遗项目" : "非遗项目《" + p.getProjectName() + "》";
            }
            case StatusConstants.Interaction.TARGET_ARTICLE -> {
                CmsArticle a = articleMapper.selectById(targetId);
                yield a == null ? "文化资讯" : "资讯《" + a.getTitle() + "》";
            }
            case StatusConstants.Interaction.TARGET_ACTIVITY -> {
                IchActivity act = activityMapper.selectById(targetId);
                yield act == null ? "非遗活动" : "活动《" + act.getTitle() + "》";
            }
            default -> "内容";
        };
    }

    private String truncate(String s, int max) {
        return s == null ? "" : (s.length() <= max ? s : s.substring(0, max) + "…");
    }

    /** 评论列表 */
    public PageResult<Interaction> comments(Integer targetType, Long targetId,
                                            long pageNum, long pageSize) {
        checkTargetType(targetType);
        long offset = (pageNum - 1) * pageSize;
        List<Interaction> records =
                interactionMapper.selectComments(targetType, targetId, offset, pageSize);
        long total = interactionMapper.countComments(targetType, targetId);
        return new PageResult<>(total, records, pageNum, pageSize);
    }

    /**
     * 删除互动记录（取消收藏 / 取消点赞 / 删除自己的评论）。
     *
     * <p>数据级权限：只能删自己的，管理员例外。
     */
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        Interaction exist = interactionMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "互动记录不存在");
        }
        SecurityUtils.checkOwnership(exist.getUserId());
        interactionMapper.deleteById(id);
        // 若删除的是收藏，回退计数
        if (exist.getType() != null
                && exist.getType() == StatusConstants.Interaction.TYPE_FAVORITE) {
            changeCounter(exist.getTargetType(), exist.getTargetId(),
                    StatusConstants.Interaction.TYPE_FAVORITE, -1);
        }
    }

    /** 管理员屏蔽 / 恢复评论 */
    @Transactional(rollbackFor = Exception.class)
    public void setCommentStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态取值只能是 0 或 1");
        }
        Interaction exist = interactionMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "评论不存在");
        }
        if (exist.getType() == null || exist.getType() != StatusConstants.Interaction.TYPE_COMMENT) {
            throw new BizException(ResultCode.PARAM_ERROR, "该记录不是评论，无法屏蔽");
        }
        Interaction update = new Interaction();
        update.setId(id);
        update.setStatus(status);
        interactionMapper.updateById(update);
    }

    // ==================== 我的收藏 ====================

    /** 我的收藏列表（跨项目与资讯） */
    public PageResult<Interaction> myFavorites(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long offset = (pageNum - 1) * pageSize;
        List<Interaction> records =
                interactionMapper.selectMyFavorites(userId, offset, pageSize);
        long total = interactionMapper.countMyFavorites(userId);
        return new PageResult<>(total, records, pageNum, pageSize);
    }

    // ==================== 级联清理 ====================

    /**
     * 清理某目标对象下的全部互动记录。
     *
     * <p>供项目 / 资讯 / 活动被删除时调用，补偿多态关联缺失的外键约束。
     */
    @Transactional(rollbackFor = Exception.class)
    public int clearByTarget(Integer targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return 0;
        }
        return interactionMapper.softDeleteByTarget(targetType, targetId);
    }

    // ==================== 内部校验 ====================

    private void checkTargetType(Integer targetType) {
        if (targetType == null || targetType < 1 || targetType > 3) {
            throw new BizException(ResultCode.PARAM_ERROR, "互动目标类型不合法");
        }
    }

    /**
     * 校验目标对象存在且可互动。
     *
     * @param onlyPublished true 表示要求目标已发布（前台互动场景）
     */
    private void checkTargetExists(Integer targetType, Long targetId, boolean onlyPublished) {
        if (targetId == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少互动目标 id");
        }
        switch (targetType) {
            case StatusConstants.Interaction.TARGET_PROJECT -> {
                IchProject project = projectMapper.selectById(targetId);
                if (project == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在");
                }
                if (onlyPublished && (project.getAuditStatus() == null
                        || project.getAuditStatus() != StatusConstants.Audit.PUBLISHED)) {
                    throw new BizException(ResultCode.NOT_FOUND, "非遗项目不存在或已下架");
                }
            }
            case StatusConstants.Interaction.TARGET_ARTICLE -> {
                CmsArticle article = articleMapper.selectById(targetId);
                if (article == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "资讯不存在");
                }
                if (onlyPublished && (article.getAuditStatus() == null
                        || article.getAuditStatus() != StatusConstants.Audit.PUBLISHED)) {
                    throw new BizException(ResultCode.NOT_FOUND, "资讯不存在或已下架");
                }
            }
            case StatusConstants.Interaction.TARGET_ACTIVITY -> {
                IchActivity activity = activityMapper.selectById(targetId);
                if (activity == null) {
                    throw new BizException(ResultCode.NOT_FOUND, "活动不存在");
                }
                // 状态 >= 1 表示已通过审核（1 报名中 / 2 报名结束 / 3 进行中 / 4 已结束），
                // 0 为待审核，此时不应对公众开放互动
                if (onlyPublished && (activity.getStatus() == null || activity.getStatus() < 1)) {
                    throw new BizException(ResultCode.NOT_FOUND, "活动不存在或未发布");
                }
            }
            default -> throw new BizException(ResultCode.PARAM_ERROR, "互动目标类型不合法");
        }
    }
}

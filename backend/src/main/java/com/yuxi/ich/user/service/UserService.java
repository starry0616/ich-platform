package com.yuxi.ich.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.article.entity.CmsArticle;
import com.yuxi.ich.article.mapper.CmsArticleMapper;
import com.yuxi.ich.activity.entity.IchActivity;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.security.LoginUser;
import com.yuxi.ich.security.SecurityUtils;
import com.yuxi.ich.user.dto.ChangePasswordRequest;
import com.yuxi.ich.user.dto.UserProfileRequest;
import com.yuxi.ich.user.dto.UserTargetItem;
import com.yuxi.ich.user.entity.SysMenu;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.entity.UserBrowseHistory;
import com.yuxi.ich.user.entity.UserFavorite;
import com.yuxi.ich.user.mapper.SysMenuMapper;
import com.yuxi.ich.user.mapper.SysUserMapper;
import com.yuxi.ich.user.mapper.UserBrowseHistoryMapper;
import com.yuxi.ich.user.mapper.UserFavoriteMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户中心服务（说明书表 6-2 接口 5—10）。
 *
 * <p>职责：个人资料、修改密码、头像、我的收藏、浏览历史、角色菜单。
 */
@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserFavoriteMapper favoriteMapper;
    private final UserBrowseHistoryMapper historyMapper;
    private final SysMenuMapper menuMapper;
    private final IchProjectMapper projectMapper;
    private final CmsArticleMapper articleMapper;
    private final IchActivityMapper activityMapper;

    public UserService(SysUserMapper userMapper, PasswordEncoder passwordEncoder,
                       UserFavoriteMapper favoriteMapper, UserBrowseHistoryMapper historyMapper,
                       SysMenuMapper menuMapper, IchProjectMapper projectMapper,
                       CmsArticleMapper articleMapper, IchActivityMapper activityMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.favoriteMapper = favoriteMapper;
        this.historyMapper = historyMapper;
        this.menuMapper = menuMapper;
        this.projectMapper = projectMapper;
        this.articleMapper = articleMapper;
        this.activityMapper = activityMapper;
    }

    /** 获取当前用户资料（证件号码脱敏展示） */
    public SysUser getProfile() {
        SysUser user = userMapper.selectById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getIdCard() != null && !user.getIdCard().isBlank()) {
            user.setIdCard(maskIdCard(user.getIdCard()));
        }
        return user;
    }

    /** 修改个人资料（仅可修改允许的字段） */
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserProfileRequest req) {
        Long userId = SecurityUtils.getUserId();
        SysUser update = new SysUser();
        update.setId(userId);
        update.setNickname(emptyToNull(req.getNickname()));
        update.setPhone(emptyToNull(req.getPhone()));
        update.setEmail(emptyToNull(req.getEmail()));
        update.setRealName(emptyToNull(req.getRealName()));
        userMapper.updateById(update);
    }

    /** 修改密码：校验原密码，新密码 BCrypt 存储 */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordRequest req) {
        SysUser user = userMapper.selectById(SecurityUtils.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR, "原密码不正确");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userMapper.updateById(update);
    }

    /** 更新头像 */
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "头像地址不能为空");
        }
        SysUser update = new SysUser();
        update.setId(SecurityUtils.getUserId());
        update.setAvatar(avatarUrl);
        userMapper.updateById(update);
    }

    // ==================== 收藏 ====================

    /** 我的收藏列表（联表带出目标名称与封面） */
    public PageResult<UserTargetItem> myFavorites(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long offset = (pageNum - 1) * pageSize;
        List<UserFavorite> favorites = favoriteMapper.selectList(
                Wrappers.<UserFavorite>lambdaQuery()
                        .eq(UserFavorite::getUserId, userId)
                        .orderByDesc(UserFavorite::getCreateTime)
                        .last("LIMIT " + offset + ", " + pageSize));
        long total = favoriteMapper.selectCount(
                Wrappers.<UserFavorite>lambdaQuery().eq(UserFavorite::getUserId, userId));
        List<UserTargetItem> items = buildTargetItems(favorites, f -> f.getCreateTime());
        return new PageResult<>(total, items, pageNum, pageSize);
    }

    // ==================== 浏览历史 ====================

    /** 记录一次浏览（详情页调用；登录用户有效） */
    @Transactional(rollbackFor = Exception.class)
    public void recordBrowse(Integer targetType, Long targetId) {
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            return;
        }
        UserBrowseHistory exist = historyMapper.selectOne(
                Wrappers.<UserBrowseHistory>lambdaQuery()
                        .eq(UserBrowseHistory::getUserId, userId)
                        .eq(UserBrowseHistory::getTargetType, targetType)
                        .eq(UserBrowseHistory::getTargetId, targetId));
        LocalDateTime now = LocalDateTime.now();
        if (exist == null) {
            UserBrowseHistory h = new UserBrowseHistory();
            h.setUserId(userId);
            h.setTargetType(targetType);
            h.setTargetId(targetId);
            h.setLastViewTime(now);
            h.setViewCount(1);
            historyMapper.insert(h);
        } else {
            UserBrowseHistory h = new UserBrowseHistory();
            h.setId(exist.getId());
            h.setLastViewTime(now);
            h.setViewCount(exist.getViewCount() == null ? 1 : exist.getViewCount() + 1);
            historyMapper.updateById(h);
        }
    }

    /** 我的浏览历史（按最近浏览时间倒序） */
    public PageResult<UserTargetItem> myHistory(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long offset = (pageNum - 1) * pageSize;
        List<UserBrowseHistory> history = historyMapper.selectList(
                Wrappers.<UserBrowseHistory>lambdaQuery()
                        .eq(UserBrowseHistory::getUserId, userId)
                        .orderByDesc(UserBrowseHistory::getLastViewTime)
                        .last("LIMIT " + offset + ", " + pageSize));
        long total = historyMapper.selectCount(
                Wrappers.<UserBrowseHistory>lambdaQuery().eq(UserBrowseHistory::getUserId, userId));
        List<UserTargetItem> items = new ArrayList<>();
        for (UserBrowseHistory h : history) {
            UserTargetItem item = resolveTarget(h.getTargetType(), h.getTargetId());
            if (item == null) {
                continue;
            }
            item.setTime(h.getLastViewTime());
            item.setViewCount(h.getViewCount());
            items.add(item);
        }
        return new PageResult<>(total, items, pageNum, pageSize);
    }

    // ==================== 菜单 ====================

    /** 当前角色菜单树（前端动态导航） */
    public List<SysMenu> myMenus() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user.getRoleId() == null) {
            return Collections.emptyList();
        }
        List<SysMenu> all = menuMapper.selectByRoleId(user.getRoleId());
        return buildMenuTree(all);
    }

    // ==================== 内部方法 ====================

    private List<UserTargetItem> buildTargetItems(List<UserFavorite> favorites,
                                                  Function<UserFavorite, LocalDateTime> timeFn) {
        List<UserTargetItem> items = new ArrayList<>();
        for (UserFavorite f : favorites) {
            UserTargetItem item = resolveTarget(f.getTargetType(), f.getTargetId());
            if (item == null) {
                continue;
            }
            item.setTime(timeFn.apply(f));
            items.add(item);
        }
        return items;
    }

    /**
     * 按目标类型联表取目标名称与封面。
     * 目标已被删除或不再可见时返回 null（列表跳过）。
     */
    private UserTargetItem resolveTarget(Integer targetType, Long targetId) {
        UserTargetItem item = new UserTargetItem();
        item.setTargetType(targetType);
        item.setTargetId(targetId);
        switch (targetType) {
            case StatusConstants.Interaction.TARGET_PROJECT -> {
                IchProject p = projectMapper.selectById(targetId);
                if (p == null) {
                    return null;
                }
                item.setTitle(p.getProjectName());
                item.setCoverUrl(p.getCoverUrl());
                item.setSummary(p.getSummary());
            }
            case StatusConstants.Interaction.TARGET_ARTICLE -> {
                CmsArticle a = articleMapper.selectById(targetId);
                if (a == null) {
                    return null;
                }
                item.setTitle(a.getTitle());
                item.setCoverUrl(a.getCoverUrl());
                item.setSummary(a.getSummary());
            }
            case StatusConstants.Interaction.TARGET_ACTIVITY -> {
                IchActivity act = activityMapper.selectById(targetId);
                if (act == null) {
                    return null;
                }
                item.setTitle(act.getTitle());
                item.setCoverUrl(act.getCoverUrl());
                item.setSummary(act.getContent() == null ? null
                        : act.getContent().length() > 200
                        ? act.getContent().substring(0, 200) : act.getContent());
            }
            default -> {
                return null;
            }
        }
        return item;
    }

    private List<SysMenu> buildMenuTree(List<SysMenu> all) {
        Map<Long, SysMenu> map = new LinkedHashMap<>();
        for (SysMenu m : all) {
            m.setChildren(new ArrayList<>());
            map.put(m.getId(), m);
        }
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : all) {
            if (m.getParentId() != null && m.getParentId() > 0 && map.containsKey(m.getParentId())) {
                map.get(m.getParentId()).getChildren().add(m);
            } else {
                roots.add(m);
            }
        }
        Comparator<SysMenu> bySort = Comparator.comparing(m -> m.getSortNo() == null ? 0 : m.getSortNo());
        roots.sort(bySort);
        roots.forEach(r -> r.getChildren().sort(bySort));
        return roots;
    }

    private String maskIdCard(String idCard) {
        if (idCard.length() <= 6) {
            return idCard;
        }
        return idCard.substring(0, 3) + "********" + idCard.substring(idCard.length() - 3);
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

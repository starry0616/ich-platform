package com.yuxi.ich.stats.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.activity.entity.ActSignup;
import com.yuxi.ich.activity.mapper.ActSignupMapper;
import com.yuxi.ich.activity.mapper.IchActivityMapper;
import com.yuxi.ich.ai.mapper.AiChatMapper;
import com.yuxi.ich.article.mapper.CmsArticleMapper;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.inheritor.mapper.IchInheritorMapper;
import com.yuxi.ich.interaction.entity.Interaction;
import com.yuxi.ich.interaction.mapper.InteractionMapper;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import com.yuxi.ich.user.mapper.SysLoginLogMapper;
import com.yuxi.ich.user.mapper.SysUserMapper;
import com.yuxi.ich.user.mapper.UserBrowseHistoryMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据统计服务（说明书 4.8 节 / 表 6-6 接口 68—71）。
 */
@Service
public class StatsService {

    private final IchProjectMapper projectMapper;
    private final IchInheritorMapper inheritorMapper;
    private final CmsArticleMapper articleMapper;
    private final IchActivityMapper activityMapper;
    private final ActSignupMapper signupMapper;
    private final SysUserMapper userMapper;
    private final AiChatMapper aiChatMapper;
    private final InteractionMapper interactionMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final UserBrowseHistoryMapper browseHistoryMapper;

    public StatsService(IchProjectMapper projectMapper, IchInheritorMapper inheritorMapper,
                        CmsArticleMapper articleMapper, IchActivityMapper activityMapper,
                        ActSignupMapper signupMapper, SysUserMapper userMapper,
                        AiChatMapper aiChatMapper, InteractionMapper interactionMapper,
                        SysLoginLogMapper loginLogMapper, UserBrowseHistoryMapper browseHistoryMapper) {
        this.projectMapper = projectMapper;
        this.inheritorMapper = inheritorMapper;
        this.articleMapper = articleMapper;
        this.activityMapper = activityMapper;
        this.signupMapper = signupMapper;
        this.userMapper = userMapper;
        this.aiChatMapper = aiChatMapper;
        this.interactionMapper = interactionMapper;
        this.loginLogMapper = loginLogMapper;
        this.browseHistoryMapper = browseHistoryMapper;
    }

    /** 前台统计概览（首页数字展示，公开） */
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("projects", projectMapper.selectCount(null));
        data.put("inheritors", inheritorMapper.selectCount(null));
        data.put("articles", articleMapper.selectCount(null));
        data.put("activities", activityMapper.selectCount(null));
        data.put("signups", signupMapper.countAll());
        data.put("favorites", countInteractionByType(StatusConstants.Interaction.TYPE_FAVORITE));
        data.put("comments", countInteractionByType(StatusConstants.Interaction.TYPE_COMMENT));
        return data;
    }

    /** 后台管理总览（接口 68） */
    public Map<String, Object> adminOverview() {
        Map<String, Object> data = overview();
        data.put("users", userMapper.selectCount(null));
        data.put("aiChats", aiChatMapper.selectCount(null));
        data.put("projectsPublished", projectMapper.selectCount(
                Wrappers.<com.yuxi.ich.project.entity.IchProject>lambdaQuery()
                        .eq(com.yuxi.ich.project.entity.IchProject::getAuditStatus,
                                StatusConstants.Audit.PUBLISHED)));
        data.put("inheritorsCertified", inheritorMapper.selectCount(
                Wrappers.<com.yuxi.ich.inheritor.entity.IchInheritor>lambdaQuery()
                        .eq(com.yuxi.ich.inheritor.entity.IchInheritor::getCertStatus,
                                StatusConstants.CertStatus.CERTIFIED)));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        data.put("todayVisits", browseHistoryMapper.selectCount(
                Wrappers.<com.yuxi.ich.user.entity.UserBrowseHistory>lambdaQuery()
                        .ge(com.yuxi.ich.user.entity.UserBrowseHistory::getLastViewTime, todayStart)));
        data.put("todaySignups", signupMapper.selectCount(
                Wrappers.<ActSignup>lambdaQuery().ge(ActSignup::getCreateTime, todayStart)));
        data.put("todayRegisters", userMapper.selectCount(
                Wrappers.<com.yuxi.ich.user.entity.SysUser>lambdaQuery()
                        .ge(com.yuxi.ich.user.entity.SysUser::getCreateTime, todayStart)));
        return data;
    }

    /** 趋势数据（接口 69）：近 N 天注册 / 登录 / 报名 / 问答 / 浏览 */
    public Map<String, Object> trends(int days) {
        if (days <= 0 || days > 90) {
            days = 7;
        }
        LocalDateTime start = LocalDate.now().minusDays(days - 1L).atStartOfDay();
        List<LocalDate> axis = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            axis.add(start.toLocalDate().plusDays(i));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", axis.stream().map(d -> d.toString()).toList());
        result.put("registers", fillDaily(axis,
                userMapper.selectRegisterCountByDay(start)));
        result.put("logins", fillDaily(axis,
                loginLogMapper.selectLoginCountByDay(start)));
        result.put("signups", fillDaily(axis,
                signupMapper.selectSignupCountByDay(start)));
        result.put("chats", fillDaily(axis,
                aiChatMapper.selectChatCountByDay(start)));
        result.put("visits", fillDaily(axis,
                browseHistoryMapper.selectBrowseCountByDay(start)));
        return result;
    }

    /** 项目按类别分布（接口 68 附） */
    public List<Map<String, Object>> categoryDistribution() {
        return projectMapper.countByCategory().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", c.getCategoryName());
                    m.put("value", c.getNum());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 传承人看板（接口 71） */
    public Map<String, Object> inheritorDashboard() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", inheritorMapper.selectCount(null));
        data.put("certStatus", inheritorMapper.countGroupByCertStatus());
        data.put("region", inheritorMapper.countGroupByRegion());
        return data;
    }

    /** 热门活动 TopN */
    public List<Map<String, Object>> topActivities(int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 5;
        }
        return signupMapper.selectTopActivities(limit);
    }

    // ==================== 内部方法 ====================

    private long countInteractionByType(int type) {
        Long count = interactionMapper.selectCount(
                Wrappers.<Interaction>lambdaQuery().eq(Interaction::getType, type));
        return count == null ? 0L : count;
    }

    /** 把数据库按天结果对齐到完整日期轴，缺日补 0 */
    private List<Long> fillDaily(List<LocalDate> axis,
                                 List<Map<String, Object>> raw) {
        Map<String, Long> byDay = new LinkedHashMap<>();
        for (Map<String, Object> row : raw) {
            Object d = row.get("d");
            Object c = row.get("c");
            if (d != null && c != null) {
                byDay.put(String.valueOf(d), Long.valueOf(String.valueOf(c)));
            }
        }
        List<Long> result = new ArrayList<>();
        for (LocalDate day : axis) {
            result.add(byDay.getOrDefault(day.toString(), 0L));
        }
        return result;
    }
}

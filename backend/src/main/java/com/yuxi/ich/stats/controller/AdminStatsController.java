package com.yuxi.ich.stats.controller;

import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.stats.service.StatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端统计接口（对应说明书表 6-6 接口 68—71，管理员）。
 */
@RestController
@RequestMapping("/api/admin/stats")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final StatsService statsService;

    public AdminStatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /** 数据总览（接口 68） */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(statsService.adminOverview());
    }

    /** 趋势图数据（接口 69） */
    @GetMapping("/trends")
    public Result<Map<String, Object>> trends(
            @RequestParam(value = "days", defaultValue = "7") int days) {
        return Result.success(statsService.trends(days));
    }

    /** 项目类别分布 */
    @GetMapping("/category")
    public Result<List<Map<String, Object>>> category() {
        return Result.success(statsService.categoryDistribution());
    }

    /** 传承人看板（接口 71） */
    @GetMapping("/inheritor")
    public Result<Map<String, Object>> inheritor() {
        return Result.success(statsService.inheritorDashboard());
    }

    /** 热门活动 TopN */
    @GetMapping("/top-activities")
    public Result<List<Map<String, Object>>> topActivities(
            @RequestParam(value = "limit", defaultValue = "5") int limit) {
        return Result.success(statsService.topActivities(limit));
    }
}

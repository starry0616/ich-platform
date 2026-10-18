package com.yuxi.ich.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.entity.SysAuditLog;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.user.entity.SysLoginLog;
import com.yuxi.ich.user.mapper.SysLoginLogMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统日志接口（对应说明书表 6-6 接口 70，管理员）。
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLogController {

    private final AuditLogService auditLogService;
    private final SysLoginLogMapper loginLogMapper;

    public AdminLogController(AuditLogService auditLogService,
                              SysLoginLogMapper loginLogMapper) {
        this.auditLogService = auditLogService;
        this.loginLogMapper = loginLogMapper;
    }

    /** 操作日志分页（接口 70：审核、禁用、类别/Banner 维护等） */
    @GetMapping("/log")
    public Result<PageResult<SysAuditLog>> log(
            @RequestParam(value = "targetType", required = false) Integer targetType,
            @RequestParam(value = "action", required = false) Integer action,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(auditLogService.page(targetType, action, pageNum, pageSize));
    }

    /** 登录日志分页 */
    @GetMapping("/login-log")
    public Result<PageResult<SysLoginLog>> loginLog(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        var query = Wrappers.<SysLoginLog>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), SysLoginLog::getUsername, keyword)
                .orderByDesc(SysLoginLog::getLoginTime)
                .last("LIMIT " + offset + ", " + pageSize);
        var countQuery = Wrappers.<SysLoginLog>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), SysLoginLog::getUsername, keyword);
        return Result.success(new PageResult<>(loginLogMapper.selectCount(countQuery),
                loginLogMapper.selectList(query), pageNum, pageSize));
    }
}

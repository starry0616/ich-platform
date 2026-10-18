package com.yuxi.ich.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.interaction.entity.Interaction;
import com.yuxi.ich.interaction.entity.InteractionReport;
import com.yuxi.ich.interaction.mapper.InteractionMapper;
import com.yuxi.ich.interaction.mapper.InteractionReportMapper;
import com.yuxi.ich.interaction.service.InteractionService;
import com.yuxi.ich.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 评论治理与举报处理接口（对应说明书表 6-4 接口 41 处理侧 + 表 6-6 接口 65，管理员）。
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    private final InteractionReportMapper reportMapper;
    private final InteractionMapper interactionMapper;
    private final InteractionService interactionService;
    private final AuditLogService auditLogService;

    public AdminReportController(InteractionReportMapper reportMapper,
                                 InteractionMapper interactionMapper,
                                 InteractionService interactionService,
                                 AuditLogService auditLogService) {
        this.reportMapper = reportMapper;
        this.interactionMapper = interactionMapper;
        this.interactionService = interactionService;
        this.auditLogService = auditLogService;
    }

    /** 举报列表分页（接口 65 附） */
    @GetMapping("/report/list")
    public Result<PageResult<Map<String, Object>>> reportList(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> records = reportMapper.selectReportPage(status, offset, pageSize);
        long total = reportMapper.countReport(status);
        return Result.success(new PageResult<>(total, records, pageNum, pageSize));
    }

    /** 处理举报：可选择同时屏蔽评论 */
    @PutMapping("/report/{id}/handle")
    public Result<Void> handle(@PathVariable("id") Long id,
                               @RequestBody Map<String, Object> body) {
        InteractionReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (report.getStatus() != null && report.getStatus() != 0) {
            throw new BizException(ResultCode.CONFLICT, "该举报已处理");
        }
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        boolean block = Boolean.TRUE.equals(body.get("blockComment"));
        if (block && report.getInteractionId() != null) {
            interactionService.setCommentStatus(report.getInteractionId(), 0);
        }
        InteractionReport update = new InteractionReport();
        update.setId(id);
        update.setStatus(1);
        update.setHandleBy(SecurityUtils.getUserId());
        update.setHandleTime(LocalDateTime.now());
        update.setRemark(remark);
        reportMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_COMMENT,
                report.getInteractionId(), AuditLogService.ACTION_OFFLINE,
                "处理举报" + (block ? "并屏蔽评论" : "") + (remark == null ? "" : "：" + remark));
        return Result.success("已处理", null);
    }

    /** 驳回举报 */
    @PutMapping("/report/{id}/reject")
    public Result<Void> reject(@PathVariable("id") Long id,
                               @RequestBody Map<String, Object> body) {
        InteractionReport report = reportMapper.selectById(id);
        if (report == null) {
            throw new BizException(ResultCode.NOT_FOUND, "举报记录不存在");
        }
        if (report.getStatus() != null && report.getStatus() != 0) {
            throw new BizException(ResultCode.CONFLICT, "该举报已处理");
        }
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        InteractionReport update = new InteractionReport();
        update.setId(id);
        update.setStatus(2);
        update.setHandleBy(SecurityUtils.getUserId());
        update.setHandleTime(LocalDateTime.now());
        update.setRemark(remark);
        reportMapper.updateById(update);
        return Result.success("已驳回", null);
    }

    /** 评论治理列表（接口 65） */
    @GetMapping("/comment/list")
    public Result<PageResult<Interaction>> commentList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "targetType", required = false) Integer targetType,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        var query = Wrappers.<Interaction>lambdaQuery()
                .eq(Interaction::getType, StatusConstants.Interaction.TYPE_COMMENT)
                .eq(targetType != null, Interaction::getTargetType, targetType)
                .like(keyword != null && !keyword.isBlank(), Interaction::getContent, keyword)
                .orderByDesc(Interaction::getCreateTime)
                .last("LIMIT " + offset + ", " + pageSize);
        var countQuery = Wrappers.<Interaction>lambdaQuery()
                .eq(Interaction::getType, StatusConstants.Interaction.TYPE_COMMENT)
                .eq(targetType != null, Interaction::getTargetType, targetType)
                .like(keyword != null && !keyword.isBlank(), Interaction::getContent, keyword);
        return Result.success(new PageResult<>(interactionMapper.selectCount(countQuery),
                interactionMapper.selectList(query), pageNum, pageSize));
    }

    /** 屏蔽 / 恢复评论（接口 65） */
    @PutMapping("/comment/{id}/status")
    public Result<Void> setCommentStatus(@PathVariable("id") Long id,
                                         @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        interactionService.setCommentStatus(id, status);
        auditLogService.record(AuditLogService.TARGET_COMMENT, id,
                status == 0 ? AuditLogService.ACTION_OFFLINE : AuditLogService.ACTION_OTHER,
                status == 0 ? "屏蔽评论" : "恢复评论");
        return Result.success(status == 0 ? "已屏蔽" : "已恢复", null);
    }
}

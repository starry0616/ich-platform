package com.yuxi.ich.inheritor.controller;

import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.inheritor.dto.InheritorApplyRequest;
import com.yuxi.ich.inheritor.dto.InheritorQueryParam;
import com.yuxi.ich.inheritor.dto.InheritorUpdateRequest;
import com.yuxi.ich.inheritor.entity.IchInheritor;
import com.yuxi.ich.inheritor.service.InheritorService;
import com.yuxi.ich.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 非遗传承人接口（对应说明书表 6-2 接口 11—12、表 6-3 接口 21—23）。
 */
@RestController
@RequestMapping("/api/inheritor")
public class InheritorController {

    private final InheritorService inheritorService;

    public InheritorController(InheritorService inheritorService) {
        this.inheritorService = inheritorService;
    }

    // ==================== 公开查询 ====================

    /**
     * 传承人名录（公开）。
     * <pre>
     * GET /api/inheritor/list?level=1&amp;region=云南&amp;keyword=李&amp;pageNum=1&amp;pageSize=12
     * </pre>
     */
    @GetMapping("/list")
    public Result<PageResult<IchInheritor>> list(InheritorQueryParam query) {
        return Result.success(inheritorService.pageForFront(query));
    }

    /** 传承人详情主页（公开，仅已认证可见） */
    @GetMapping("/{id}")
    public Result<IchInheritor> detail(@PathVariable("id") Long id) {
        return Result.success(inheritorService.getDetail(id, true, true));
    }

    // ==================== 资质申请 ====================

    /**
     * 提交传承人资质申请（需登录）。
     *
     * <p>提交后状态为待审核；曾被驳回的可修改后重新提交。
     */
    @PostMapping("/apply")
    public Result<Long> apply(@Valid @RequestBody InheritorApplyRequest request) {
        Long userId = SecurityUtils.getUserId();
        return Result.success("申请已提交，等待审核", inheritorService.apply(userId, request));
    }

    /** 查询自己的申请进度 */
    @GetMapping("/apply/status")
    public Result<IchInheritor> applyStatus() {
        return Result.success(inheritorService.getMyProfile(SecurityUtils.getUserId()));
    }

    // ==================== 传承人维护自己的档案 ====================

    /** 维护个人主页（仅已认证传承人） */
    @PutMapping("/profile")
    @PreAuthorize("hasAnyRole('INHERITOR', 'ADMIN')")
    public Result<Void> updateMyProfile(@Valid @RequestBody InheritorUpdateRequest request) {
        inheritorService.updateMyProfile(SecurityUtils.getUserId(), request);
        return Result.success("保存成功", null);
    }

    // ==================== 后台管理 ====================

    /** 认证待审列表（管理员） */
    @GetMapping("/admin/audit/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<IchInheritor>> auditList(InheritorQueryParam query) {
        return Result.success(inheritorService.pageForAdmin(query));
    }

    /**
     * 认证审核（管理员）。
     *
     * @param certStatus 1 通过（同时把账号提升为传承人角色），2 驳回
     */
    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> audit(@PathVariable("id") Long id,
                              @RequestParam("certStatus") Integer certStatus) {
        inheritorService.audit(id, certStatus);
        return Result.success("审核完成", null);
    }

    /** 管理员维护档案 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateByAdmin(@PathVariable("id") Long id,
                                      @Valid @RequestBody InheritorUpdateRequest request) {
        inheritorService.updateByAdmin(id, request);
        return Result.success("保存成功", null);
    }

    /** 删除档案（逻辑删除，管理员） */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> delete(@PathVariable("id") Long id) {
        inheritorService.delete(id);
        return Result.success("删除成功", null);
    }
}

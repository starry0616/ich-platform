package com.yuxi.ich.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.entity.SysBanner;
import com.yuxi.ich.admin.mapper.SysBannerMapper;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.common.response.ResultCode;
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
 * 首页推荐位管理接口（对应说明书表 6-6 接口 66—67，管理员）。
 */
@RestController
@RequestMapping("/api/admin/banner")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBannerController {

    private final SysBannerMapper bannerMapper;
    private final AuditLogService auditLogService;

    public AdminBannerController(SysBannerMapper bannerMapper,
                                 AuditLogService auditLogService) {
        this.bannerMapper = bannerMapper;
        this.auditLogService = auditLogService;
    }

    /** Banner 分页（接口 66） */
    @GetMapping("/list")
    public Result<PageResult<SysBanner>> list(
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        var query = Wrappers.<SysBanner>lambdaQuery()
                .eq(status != null, SysBanner::getStatus, status)
                .orderByAsc(SysBanner::getSortNo)
                .last("LIMIT " + offset + ", " + pageSize);
        var countQuery = Wrappers.<SysBanner>lambdaQuery()
                .eq(status != null, SysBanner::getStatus, status);
        return Result.success(new PageResult<>(bannerMapper.selectCount(countQuery),
                bannerMapper.selectList(query), pageNum, pageSize));
    }

    /** 新增 Banner（接口 66） */
    @PostMapping
    public Result<Long> create(@RequestBody SysBanner banner) {
        validate(banner);
        banner.setId(null);
        if (banner.getStatus() == null) {
            banner.setStatus(1);
        }
        bannerMapper.insert(banner);
        auditLogService.record(AuditLogService.TARGET_PROJECT, banner.getId(),
                AuditLogService.ACTION_OTHER, "新增推荐位：" + banner.getTitle());
        return Result.success("创建成功", banner.getId());
    }

    /** 修改 Banner（接口 67） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody SysBanner banner) {
        if (bannerMapper.selectById(id) == null) {
            throw new BizException(ResultCode.NOT_FOUND, "推荐位不存在");
        }
        validate(banner);
        banner.setId(id);
        bannerMapper.updateById(banner);
        auditLogService.record(AuditLogService.TARGET_PROJECT, id,
                AuditLogService.ACTION_OTHER, "修改推荐位：" + banner.getTitle());
        return Result.success("更新成功", null);
    }

    /** 删除 Banner */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        SysBanner exist = bannerMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "推荐位不存在");
        }
        bannerMapper.deleteById(id);
        auditLogService.record(AuditLogService.TARGET_PROJECT, id,
                AuditLogService.ACTION_OTHER, "删除推荐位：" + exist.getTitle());
        return Result.success("删除成功", null);
    }

    private void validate(SysBanner banner) {
        if (banner.getTitle() == null || banner.getTitle().isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "标题不能为空");
        }
        if (banner.getImageUrl() == null || banner.getImageUrl().isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "图片地址不能为空");
        }
        if (banner.getTargetType() == null || banner.getTargetType() < 1 || banner.getTargetType() > 4) {
            throw new BizException(ResultCode.PARAM_ERROR, "跳转类型不合法");
        }
        if (banner.getTargetType() == 4) {
            if (banner.getLinkUrl() == null || banner.getLinkUrl().isBlank()) {
                throw new BizException(ResultCode.PARAM_ERROR, "外链跳转需提供链接地址");
            }
        } else if (banner.getTargetId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少跳转对象 id");
        }
        if (banner.getSortNo() == null) {
            banner.setSortNo(0);
        }
        if (banner.getStatus() == null || (banner.getStatus() != 0 && banner.getStatus() != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态取值只能是 0 或 1");
        }
    }
}

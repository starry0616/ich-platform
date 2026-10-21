package com.yuxi.ich.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.project.entity.IchCategory;
import com.yuxi.ich.project.entity.IchProject;
import com.yuxi.ich.project.mapper.IchCategoryMapper;
import com.yuxi.ich.project.mapper.IchProjectMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 类别管理接口（对应说明书表 6-6 接口 63—64，管理员）。
 */
@RestController
@RequestMapping("/api/admin/category")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final IchCategoryMapper categoryMapper;
    private final IchProjectMapper projectMapper;
    private final AuditLogService auditLogService;

    public AdminCategoryController(IchCategoryMapper categoryMapper,
                                   IchProjectMapper projectMapper,
                                   AuditLogService auditLogService) {
        this.categoryMapper = categoryMapper;
        this.projectMapper = projectMapper;
        this.auditLogService = auditLogService;
    }

    /** 类别全量列表（含停用，供后台维护） */
    @GetMapping("/list")
    public Result<List<IchCategory>> list() {
        return Result.success(categoryMapper.selectList(
                Wrappers.<IchCategory>lambdaQuery()
                        .orderByAsc(IchCategory::getSortNo)));
    }

    /** 新增类别（接口 63） */
    @PostMapping
    public Result<Long> create(@RequestBody IchCategory category) {
        validate(category, null);
        category.setId(null);
        if (category.getStatus() == null) {
            category.setStatus(1);
        }
        categoryMapper.insert(category);
        auditLogService.record(AuditLogService.TARGET_PROJECT, category.getId(),
                AuditLogService.ACTION_OTHER, "新增类别：" + category.getCategoryName());
        return Result.success("创建成功", category.getId());
    }

    /** 修改类别（接口 64） */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable("id") Long id,
                               @RequestBody IchCategory category) {
        IchCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "类别不存在");
        }
        validate(category, id);
        category.setId(id);
        categoryMapper.updateById(category);
        auditLogService.record(AuditLogService.TARGET_PROJECT, id,
                AuditLogService.ACTION_OTHER, "修改类别：" + category.getCategoryName());
        return Result.success("更新成功", null);
    }

    /** 删除类别（有项目关联时禁止删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable("id") Long id) {
        IchCategory exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "类别不存在");
        }
        Long children = categoryMapper.selectCount(
                Wrappers.<IchCategory>lambdaQuery().eq(IchCategory::getParentId, id));
        if (children != null && children > 0) {
            throw new BizException(ResultCode.CONFLICT, "请先删除该类别下的子类别");
        }
        Long projects = projectMapper.selectCount(
                Wrappers.<IchProject>lambdaQuery().eq(IchProject::getCategoryId, id));
        if (projects != null && projects > 0) {
            throw new BizException(ResultCode.CONFLICT, "该类别下存在项目，无法删除");
        }
        categoryMapper.deleteById(id);
        auditLogService.record(AuditLogService.TARGET_PROJECT, id,
                AuditLogService.ACTION_OTHER, "删除类别：" + exist.getCategoryName());
        return Result.success("删除成功", null);
    }

    /**
     * 新增/更新前校验。
     *
     * @param selfId 更新时传入当前类别 id，新增传 null
     */
    private void validate(IchCategory category, Long selfId) {
        if (category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            throw new BizException(ResultCode.PARAM_ERROR, "类别名称不能为空");
        }
        String name = category.getCategoryName().trim();
        Long parentId = category.getParentId() == null ? 0L : category.getParentId();
        if (parentId != 0) {
            IchCategory parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ResultCode.PARAM_ERROR, "父级类别不存在");
            }
        }
        // 更新时拒绝把父级设为自身（自引用）
        if (selfId != null && selfId.equals(parentId)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能将类别挂到自身作为父级");
        }
        // 名称全局唯一：避免重名门类导致项目归属歧义；更新时排除自身
        Long dup = categoryMapper.selectCount(
                Wrappers.<IchCategory>lambdaQuery()
                        .eq(IchCategory::getCategoryName, name)
                        .ne(selfId != null, IchCategory::getId, selfId));
        if (dup != null && dup > 0) {
            throw new BizException(ResultCode.CONFLICT, "类别名称已存在");
        }
        // 更新时沿新父级链向上检查，禁止间接循环（如 A→B 后又把 B 的父级设为 A）
        if (selfId != null && parentId != 0) {
            assertNoCycle(selfId, parentId);
        }
        category.setCategoryName(name);
        category.setParentId(parentId);
        if (category.getSortNo() == null) {
            category.setSortNo(0);
        }
    }

    /** 沿父级链向上遍历，若回到自身则说明形成循环；带步数上限防止异常链路死循环 */
    private void assertNoCycle(Long selfId, Long parentId) {
        Long cursor = parentId;
        int guard = 0;
        while (cursor != null && cursor != 0L) {
            if (selfId.equals(cursor)) {
                throw new BizException(ResultCode.PARAM_ERROR, "不能将类别挂到自身后代，会形成循环层级");
            }
            if (++guard > 100) {
                throw new BizException(ResultCode.PARAM_ERROR, "类别层级链路异常");
            }
            IchCategory parent = categoryMapper.selectById(cursor);
            if (parent == null) {
                break;
            }
            cursor = parent.getParentId();
        }
    }
}

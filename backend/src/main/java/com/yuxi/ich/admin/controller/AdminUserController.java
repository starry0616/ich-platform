package com.yuxi.ich.admin.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.admin.service.AuditLogService;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.security.SecurityUtils;
import com.yuxi.ich.user.entity.SysRole;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.mapper.SysRoleMapper;
import com.yuxi.ich.user.mapper.SysUserMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户管理接口（对应说明书表 6-6 接口 54—56，管理员）。
 */
@RestController
@RequestMapping("/api/admin/user")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final AuditLogService auditLogService;

    public AdminUserController(SysUserMapper userMapper, SysRoleMapper roleMapper,
                               AuditLogService auditLogService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.auditLogService = auditLogService;
    }

    /** 用户分页（接口 54） */
    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        var query = Wrappers.<SysUser>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getNickname, keyword)
                                .or().like(SysUser::getPhone, keyword))
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getCreateTime)
                .last("LIMIT " + offset + ", " + pageSize);
        var countQuery = Wrappers.<SysUser>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(SysUser::getUsername, keyword)
                                .or().like(SysUser::getNickname, keyword)
                                .or().like(SysUser::getPhone, keyword))
                .eq(status != null, SysUser::getStatus, status);
        return Result.success(new PageResult<>(userMapper.selectCount(countQuery),
                userMapper.selectList(query), pageNum, pageSize));
    }

    /** 启用 / 禁用账号（接口 55） */
    @PutMapping("/{id}/status")
    public Result<Void> setStatus(@PathVariable("id") Long id,
                                  @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态取值只能是 0 或 1");
        }
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long me = SecurityUtils.getUserId();
        if (me != null && me.equals(id)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能操作自己的账号状态");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_USER, id,
                status == 0 ? AuditLogService.ACTION_DISABLE : AuditLogService.ACTION_OTHER,
                (status == 0 ? "禁用账号：" : "启用账号：") + exist.getUsername());
        return Result.success(status == 0 ? "已禁用" : "已启用", null);
    }

    /** 修改用户角色（接口 56） */
    @PutMapping("/{id}/role")
    public Result<Void> setRole(@PathVariable("id") Long id,
                                @RequestBody Map<String, Long> body) {
        Long roleId = body.get("roleId");
        if (roleId == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少角色 id");
        }
        SysUser exist = userMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "用户不存在");
        }
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(ResultCode.NOT_FOUND, "角色不存在");
        }
        Long me = SecurityUtils.getUserId();
        if (me != null && me.equals(id)) {
            throw new BizException(ResultCode.PARAM_ERROR, "不能修改自己的角色");
        }
        SysUser update = new SysUser();
        update.setId(id);
        update.setRoleId(roleId);
        userMapper.updateById(update);
        auditLogService.record(AuditLogService.TARGET_USER, id, AuditLogService.ACTION_OTHER,
                "调整角色：" + exist.getUsername() + " → " + role.getRoleName());
        return Result.success("角色已更新", null);
    }

    /** 全部角色（供角色下拉） */
    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.success(roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().orderByAsc(SysRole::getId)));
    }
}

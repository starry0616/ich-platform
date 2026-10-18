package com.yuxi.ich.admin.service;

import com.yuxi.ich.admin.entity.SysAuditLog;
import com.yuxi.ich.admin.mapper.SysAuditLogMapper;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 审计日志服务（说明书 7.3 / 7.4 节）。
 *
 * <p>审核动作、账号禁用等关键操作统一通过 {@link #record} 落库，
 * 供管理员在「系统日志」页查询（接口 70 /api/admin/log）。
 */
@Service
public class AuditLogService {

    /** 目标类型：1 项目，2 资讯，3 活动，4 传承人，5 评论，6 用户 */
    public static final int TARGET_PROJECT = 1;
    public static final int TARGET_ARTICLE = 2;
    public static final int TARGET_ACTIVITY = 3;
    public static final int TARGET_INHERITOR = 4;
    public static final int TARGET_COMMENT = 5;
    public static final int TARGET_USER = 6;

    /** 动作：1 通过，2 驳回，3 下架，4 禁用，5 其他 */
    public static final int ACTION_PASS = 1;
    public static final int ACTION_REJECT = 2;
    public static final int ACTION_OFFLINE = 3;
    public static final int ACTION_DISABLE = 4;
    public static final int ACTION_OTHER = 5;

    private final SysAuditLogMapper auditLogMapper;

    public AuditLogService(SysAuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /** 写入一条审计日志（从当前登录上下文取审核人） */
    @Transactional(rollbackFor = Exception.class)
    public void record(int targetType, Long targetId, int action, String remark) {
        SysAuditLog log = new SysAuditLog();
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setAction(action);
        log.setRemark(remark);
        log.setAuditorId(SecurityUtils.getUserIdOrNull());
        auditLogMapper.insert(log);
    }

    /** 操作日志分页（管理员） */
    public PageResult<SysAuditLog> page(Integer targetType, Integer action,
                                        long pageNum, long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        List<SysAuditLog> records =
                auditLogMapper.selectLogPage(targetType, action, offset, pageSize);
        long total = auditLogMapper.countLog(targetType, action);
        return new PageResult<>(total, records, pageNum, pageSize);
    }
}

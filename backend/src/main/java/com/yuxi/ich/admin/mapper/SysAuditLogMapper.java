package com.yuxi.ich.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.admin.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 内容审核记录表 Mapper。
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    /**
     * 分页查询操作日志（联表带出审核人姓名）。
     */
    @Select("<script>" +
            "SELECT l.*, u.nickname AS auditor_name FROM sys_audit_log l " +
            "LEFT JOIN sys_user u ON u.id = l.auditor_id " +
            "<where>" +
            "<if test='targetType != null'> AND l.target_type = #{targetType}</if>" +
            "<if test='action != null'> AND l.action = #{action}</if>" +
            "</where>" +
            " ORDER BY l.create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<SysAuditLog> selectLogPage(@Param("targetType") Integer targetType,
                                    @Param("action") Integer action,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

    /**
     * 统计操作日志条数。
     */
    @Select("<script>" +
            "SELECT COUNT(*) FROM sys_audit_log l " +
            "<where>" +
            "<if test='targetType != null'> AND l.target_type = #{targetType}</if>" +
            "<if test='action != null'> AND l.action = #{action}</if>" +
            "</where>" +
            "</script>")
    long countLog(@Param("targetType") Integer targetType,
                  @Param("action") Integer action);
}

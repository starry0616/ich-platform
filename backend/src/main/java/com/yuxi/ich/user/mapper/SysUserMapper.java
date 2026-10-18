package com.yuxi.ich.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户 Mapper。单表操作全部由 BaseMapper 提供。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 最近 N 天每日注册用户数（数据看板） */
    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS c FROM sys_user " +
            "WHERE deleted = 0 AND create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) ORDER BY d")
    List<Map<String, Object>> selectRegisterCountByDay(@Param("startDate") LocalDateTime startDate);
}

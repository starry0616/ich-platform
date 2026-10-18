package com.yuxi.ich.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.user.entity.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 登录日志表 Mapper。
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {

    /** 最近 N 天每日登录次数（数据看板） */
    @Select("SELECT DATE(login_time) AS d, COUNT(*) AS c FROM sys_login_log " +
            "WHERE login_time >= #{startDate} " +
            "GROUP BY DATE(login_time) ORDER BY d")
    List<Map<String, Object>> selectLoginCountByDay(@Param("startDate") LocalDateTime startDate);
}

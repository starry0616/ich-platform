package com.yuxi.ich.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.user.entity.UserBrowseHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户浏览历史表 Mapper。
 */
@Mapper
public interface UserBrowseHistoryMapper extends BaseMapper<UserBrowseHistory> {

    /** 最近 N 天每日浏览次数（数据看板） */
    @Select("SELECT DATE(last_view_time) AS d, COUNT(*) AS c FROM user_browse_history " +
            "WHERE last_view_time >= #{startDate} " +
            "GROUP BY DATE(last_view_time) ORDER BY d")
    List<Map<String, Object>> selectBrowseCountByDay(@Param("startDate") LocalDateTime startDate);
}

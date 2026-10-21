package com.yuxi.ich.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.activity.entity.ActSignup;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 活动报名 Mapper。
 */
@Mapper
public interface ActSignupMapper extends BaseMapper<ActSignup> {

    /** 某活动的报名名单（供传承人查看与现场核销） */
    List<ActSignup> selectByActivity(@Param("activityId") Long activityId,
                                     @Param("status") Integer status,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /** 某活动的报名人数（按状态筛选） */
    long countByActivity(@Param("activityId") Long activityId,
                         @Param("status") Integer status);

    /** 我的报名列表（带活动名称、时间与地点） */
    List<ActSignup> selectMySignups(@Param("userId") Long userId,
                                    @Param("offset") long offset,
                                    @Param("limit") long limit);

    /** 我的报名总数 */
    long countMySignups(@Param("userId") Long userId);

    /** 某活动的已核销人数（统计核销率用） */
    @Select("""
            SELECT COUNT(*) FROM act_signup
            WHERE activity_id = #{activityId} AND checkin_status = 1 AND deleted = 0
            """)
    long countCheckedIn(@Param("activityId") Long activityId);

    /** 报名总人次（数据看板用） */
    @Select("SELECT COUNT(*) FROM act_signup WHERE deleted = 0")
    long countAll();

    /** 最近 N 天每日报名次数（数据看板） */
    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS c FROM act_signup " +
            "WHERE deleted = 0 AND create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) ORDER BY d")
    List<Map<String, Object>> selectSignupCountByDay(@Param("startDate") LocalDateTime startDate);

    /**
     * 报名最热门的活动 TopN（管理员数据看板，按报名人数）。
     *
     * <p>仅纳入有效/历史活动：1报名中、2报名结束、3进行中、4已结束；
     * 排除 0待审核 与 5已取消/驳回，避免取消活动以"热门"名义出现在看板。
     * 这是后台热度看板（含历史活动），不套用公开可报名口径。
     */
    @Select("SELECT a.id, a.title, a.cover_url AS coverUrl, a.quota, COUNT(s.id) AS signupCount " +
            "FROM ich_activity a LEFT JOIN act_signup s ON s.activity_id = a.id AND s.deleted = 0 " +
            "WHERE a.deleted = 0 AND a.status IN (1,2,3,4) " +
            "GROUP BY a.id, a.title, a.cover_url, a.quota " +
            "ORDER BY signupCount DESC, a.id ASC LIMIT #{limit}")
    List<Map<String, Object>> selectTopActivities(@Param("limit") int limit);
}

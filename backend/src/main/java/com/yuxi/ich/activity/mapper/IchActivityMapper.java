package com.yuxi.ich.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.activity.dto.ActivityQueryParam;
import com.yuxi.ich.activity.entity.IchActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 非遗活动 Mapper。
 */
@Mapper
public interface IchActivityMapper extends BaseMapper<IchActivity> {

    /** 分页查询（带项目名与传承人姓名） */
    List<IchActivity> selectActivityPage(@Param("q") ActivityQueryParam query,
                                         @Param("offset") long offset,
                                         @Param("limit") long limit);

    /** 与分页查询同条件的总数 */
    long countActivity(@Param("q") ActivityQueryParam query);

    /** 详情 */
    IchActivity selectActivityDetail(@Param("id") Long id);

    /**
     * 原子扣减名额 —— 防超卖的核心 SQL。
     *
     * <p>三重保障合并在这一条语句里：
     * <ol>
     *   <li>{@code signed < quota} —— 条件更新，名额已满则影响行数为 0，
     *       数据库层面保证不会扣成负数</li>
     *   <li>{@code version = #{version}} —— 乐观锁，版本被别人改过则更新失败，
     *       调用方重试即可</li>
     *   <li>{@code version = version + 1} —— 成功后递增版本，使并发的其他事务失败</li>
     * </ol>
     *
     * @return 影响行数；1 表示扣减成功，0 表示名额已满或版本冲突
     */
    @Update("""
            UPDATE ich_activity
            SET signed = signed + 1, version = version + 1
            WHERE id = #{id}
              AND deleted = 0
              AND signed < quota
              AND version = #{version}
            """)
    int tryOccupyQuota(@Param("id") Long id, @Param("version") Integer version);

    /**
     * 回退名额（取消报名时使用）。
     *
     * <p>同样用条件更新，并用 GREATEST 兜底避免计数被减成负数。
     */
    @Update("""
            UPDATE ich_activity
            SET signed = GREATEST(signed - 1, 0), version = version + 1
            WHERE id = #{id} AND deleted = 0
            """)
    int releaseQuota(@Param("id") Long id);

    /** 查询名额余量（用于报名前的快速校验与前端展示） */
    @Select("""
            SELECT quota - signed FROM ich_activity
            WHERE id = #{id} AND deleted = 0
            """)
    Integer selectRemaining(@Param("id") Long id);

    /** 活动总数（数据看板用） */
    @Select("SELECT COUNT(*) FROM ich_activity WHERE deleted = 0")
    long countAll();
}

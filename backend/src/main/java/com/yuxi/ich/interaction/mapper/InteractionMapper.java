package com.yuxi.ich.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.interaction.entity.Interaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 互动记录 Mapper。
 */
@Mapper
public interface InteractionMapper extends BaseMapper<Interaction> {

    /**
     * 查询某目标对象的评论列表（顶层评论 + 其回复），带评论人昵称与头像。
     *
     * <p>只做一层回复，不做无限嵌套——嵌套会显著增加前端复杂度，
     * 而本项目按说明书约定「顶层评论 + 一层回复」即可满足需求。
     */
    List<Interaction> selectComments(@Param("targetType") Integer targetType,
                                     @Param("targetId") Long targetId,
                                     @Param("offset") long offset,
                                     @Param("limit") long limit);

    /** 评论总数 */
    long countComments(@Param("targetType") Integer targetType,
                       @Param("targetId") Long targetId);

    /**
     * 我的收藏列表（带目标对象的标题与封面）。
     *
     * <p>多态关联的代价：无法 JOIN 一张表就拿到标题，
     * 因此用 UNION ALL 按 target_type 分别关联三张表。
     */
    List<Interaction> selectMyFavorites(@Param("userId") Long userId,
                                        @Param("offset") long offset,
                                        @Param("limit") long limit);

    /** 我的收藏总数 */
    long countMyFavorites(@Param("userId") Long userId);

    /** 某用户对某目标是否已收藏/点赞（幂等判断） */
    @Select("""
            SELECT COUNT(*) FROM interaction
            WHERE user_id = #{userId} AND target_type = #{targetType}
              AND target_id = #{targetId} AND type = #{type} AND deleted = 0
            """)
    int existsInteraction(@Param("userId") Long userId,
                          @Param("targetType") Integer targetType,
                          @Param("targetId") Long targetId,
                          @Param("type") Integer type);

    /** 某目标已屏蔽的评论数（后台治理统计） */
    @Select("""
            SELECT COUNT(*) FROM interaction
            WHERE target_type = #{targetType} AND target_id = #{targetId}
              AND type = 3 AND status = 0 AND deleted = 0
            """)
    int countBlockedComments(@Param("targetType") Integer targetType,
                             @Param("targetId") Long targetId);

    /** 逻辑删除某目标下的全部互动记录（删除项目/资讯/活动时级联清理） */
    @Update("""
            UPDATE interaction SET deleted = 1
            WHERE target_type = #{targetType} AND target_id = #{targetId} AND deleted = 0
            """)
    int softDeleteByTarget(@Param("targetType") Integer targetType,
                           @Param("targetId") Long targetId);
}

package com.yuxi.ich.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.project.dto.ProjectQueryParam;
import com.yuxi.ich.project.entity.IchProject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 非遗项目 Mapper。
 *
 * <p>继承 BaseMapper 后即拥有单表增删改查能力，无需手写 SQL。
 * 只有联表查询、聚合统计等场景才需要自定义方法。
 */
@Mapper
public interface IchProjectMapper extends BaseMapper<IchProject> {

    /**
     * 分页查询项目（带类别名称）。
     *
     * <p>筛选条件是动态的，用 XML 动态 SQL 实现，
     * 见 resources/mapper/IchProjectMapper.xml。
     */
    List<IchProject> selectProjectPage(@Param("q") ProjectQueryParam query,
                                       @Param("offset") long offset,
                                       @Param("limit") long limit);

    /** 与分页查询同条件的总数统计 */
    long countProject(@Param("q") ProjectQueryParam query);

    /** 按 id 查询详情（带类别名称与富文本字段） */
    IchProject selectProjectDetail(@Param("id") Long id);

    /** 热门项目排行：仅已发布，按浏览量倒序 */
    List<IchProject> selectHotProjects(@Param("limit") int limit);

    /**
     * 浏览量自增。
     *
     * <p>用 SQL 原地自增，而不是「查出来 +1 再写回」，
     * 避免并发下的更新丢失。
     */
    @Update("UPDATE ich_project SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);

    /** 按类别统计项目数，用于数据看板 */
    @Select("""
            SELECT c.category_name AS categoryName, COUNT(p.id) AS num
            FROM ich_category c
            LEFT JOIN ich_project p ON p.category_id = c.id AND p.deleted = 0
            WHERE c.deleted = 0
            GROUP BY c.id, c.category_name
            ORDER BY c.sort_no ASC
            """)
    List<CategoryCount> countByCategory();
}

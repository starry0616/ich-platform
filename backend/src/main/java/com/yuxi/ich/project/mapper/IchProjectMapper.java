package com.yuxi.ich.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.project.dto.ProjectQueryParam;
import com.yuxi.ich.project.dto.RelatedInheritorView;
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
     * 只读：通过关联表查询某项目的已认证传承人（用于项目详情公开展示）。
     *
     * <p>关联表 {@code ich_project_inheritor} 无 deleted 列，软删/未认证的过滤
     * 必须落在主表 JOIN 条件上。本查询仅用于展示，不参与管理归属判定。
     */
    @Select("""
            SELECT h.id, h.name, h.level, h.region, h.cert_no AS certNo
            FROM ich_project_inheritor r
            JOIN ich_inheritor h
              ON h.id = r.inheritor_id AND h.deleted = 0 AND h.cert_status = 1
            WHERE r.project_id = #{projectId}
            ORDER BY h.id
            """)
    List<RelatedInheritorView> selectRelatedInheritors(@Param("projectId") Long projectId);

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

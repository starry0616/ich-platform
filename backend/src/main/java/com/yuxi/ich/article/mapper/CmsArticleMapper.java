package com.yuxi.ich.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.article.dto.ArticleQueryParam;
import com.yuxi.ich.article.entity.CmsArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 文化资讯 Mapper。
 */
@Mapper
public interface CmsArticleMapper extends BaseMapper<CmsArticle> {

    /** 分页查询（带分类名、作者昵称、关联项目名） */
    List<CmsArticle> selectArticlePage(@Param("q") ArticleQueryParam query,
                                       @Param("offset") long offset,
                                       @Param("limit") long limit);

    /** 与分页查询同条件的总数 */
    long countArticle(@Param("q") ArticleQueryParam query);

    /** 详情 */
    CmsArticle selectArticleDetail(@Param("id") Long id);

    /** 同一项目下的相关资讯，用于详情页推荐 */
    List<CmsArticle> selectRelated(@Param("projectId") Long projectId,
                                   @Param("excludeId") Long excludeId,
                                   @Param("limit") int limit);

    /** 浏览量自增 */
    @Update("UPDATE cms_article SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);
}

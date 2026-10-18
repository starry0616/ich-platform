package com.yuxi.ich.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.article.entity.CmsArticleCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资讯分类 Mapper。字典数据，以查询为主。
 */
@Mapper
public interface CmsArticleCategoryMapper extends BaseMapper<CmsArticleCategory> {
}

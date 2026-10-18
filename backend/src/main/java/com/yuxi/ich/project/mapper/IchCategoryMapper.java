package com.yuxi.ich.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.project.entity.IchCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 非遗类别 Mapper。类别为固定十大门类，查询为主。
 */
@Mapper
public interface IchCategoryMapper extends BaseMapper<IchCategory> {
}

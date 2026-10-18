package com.yuxi.ich.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.user.entity.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏表 Mapper。
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}

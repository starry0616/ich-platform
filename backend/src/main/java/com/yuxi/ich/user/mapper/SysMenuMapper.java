package com.yuxi.ich.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.user.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单权限表 Mapper。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 按角色查询菜单。
     *
     * <p>联表 sys_role_menu 取出该角色可见的菜单，按 sort_no 升序返回。
     */
    @Select("SELECT m.* FROM sys_menu m " +
            "JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "WHERE rm.role_id = #{roleId} AND m.visible = 1 AND m.deleted = 0 " +
            "ORDER BY m.sort_no ASC")
    List<SysMenu> selectByRoleId(@Param("roleId") Long roleId);
}

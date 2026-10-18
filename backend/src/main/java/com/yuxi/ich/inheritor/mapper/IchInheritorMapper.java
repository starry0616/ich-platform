package com.yuxi.ich.inheritor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.inheritor.dto.InheritorQueryParam;
import com.yuxi.ich.inheritor.entity.IchInheritor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 非遗传承人 Mapper。
 */
@Mapper
public interface IchInheritorMapper extends BaseMapper<IchInheritor> {

    /** 分页查询（带关联项目名称与账号用户名） */
    List<IchInheritor> selectInheritorPage(@Param("q") InheritorQueryParam query,
                                           @Param("offset") long offset,
                                           @Param("limit") long limit);

    /** 与分页查询同条件的总数 */
    long countInheritor(@Param("q") InheritorQueryParam query);

    /** 详情（带关联项目名称） */
    IchInheritor selectInheritorDetail(@Param("id") Long id);

    /*
     * 说明：曾在这里声明 selectByUserId 并在 XML 中实现，但漏写了对应 <select>，
     * 运行期报 "Invalid bound statement (not found)"。
     * 改为直接用 MyBatis-Plus 的 Wrapper 查询（见 InheritorService#findByUserId），
     * 少一处 XML 与接口不同步的风险。
     */

    /** 主页访问量自增 */
    @Update("UPDATE ich_inheritor SET view_count = view_count + 1 WHERE id = #{id} AND deleted = 0")
    int incrementViewCount(@Param("id") Long id);

    /** 按认证状态分布（数据看板） */
    @Select("SELECT cert_status AS status, COUNT(*) AS num FROM ich_inheritor " +
            "WHERE deleted = 0 GROUP BY cert_status")
    List<Map<String, Object>> countGroupByCertStatus();

    /** 按地域分布（数据看板） */
    @Select("SELECT region, COUNT(*) AS num FROM ich_inheritor " +
            "WHERE deleted = 0 AND region IS NOT NULL AND region != '' " +
            "GROUP BY region ORDER BY num DESC")
    List<Map<String, Object>> countGroupByRegion();
}

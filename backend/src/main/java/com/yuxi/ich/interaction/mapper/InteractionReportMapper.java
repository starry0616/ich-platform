package com.yuxi.ich.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.interaction.entity.InteractionReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 评论举报表 Mapper。
 */
@Mapper
public interface InteractionReportMapper extends BaseMapper<InteractionReport> {

    /** 举报列表分页（联表带出被举报评论内容与举报人昵称） */
    @Select("<script>" +
            "SELECT r.*, i.content AS comment_content, i.target_type, i.target_id, " +
            "u.nickname AS reporter_name, h.nickname AS handle_name " +
            "FROM interaction_report r " +
            "LEFT JOIN interaction i ON i.id = r.interaction_id " +
            "LEFT JOIN sys_user u ON u.id = r.user_id " +
            "LEFT JOIN sys_user h ON h.id = r.handle_by " +
            "<where>" +
            "<if test='status != null'> AND r.status = #{status}</if>" +
            "</where>" +
            " ORDER BY r.create_time DESC LIMIT #{offset}, #{limit}" +
            "</script>")
    List<Map<String, Object>> selectReportPage(@Param("status") Integer status,
                                               @Param("offset") long offset,
                                               @Param("limit") long limit);

    /** 举报总数 */
    @Select("<script>" +
            "SELECT COUNT(*) FROM interaction_report r " +
            "<where>" +
            "<if test='status != null'> AND r.status = #{status}</if>" +
            "</where>" +
            "</script>")
    long countReport(@Param("status") Integer status);
}

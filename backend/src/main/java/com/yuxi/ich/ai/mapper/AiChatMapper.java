package com.yuxi.ich.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.ai.entity.AiChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI 问答记录表 Mapper。
 */
@Mapper
public interface AiChatMapper extends BaseMapper<AiChat> {

    /** 最近 N 天每日提问次数（数据看板） */
    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS c FROM ai_chat " +
            "WHERE deleted = 0 AND create_time >= #{startDate} " +
            "GROUP BY DATE(create_time) ORDER BY d")
    List<Map<String, Object>> selectChatCountByDay(@Param("startDate") LocalDateTime startDate);

    /**
     * 无结果问题清单（top_score 低于阈值或命中数为 0 的提问），
     * 供管理员补充知识库（说明书 4.8 节）。
     */
    @Select("SELECT question, COUNT(*) AS ask_count, MAX(create_time) AS last_ask_time " +
            "FROM ai_chat " +
            "WHERE deleted = 0 AND (hit_count = 0 OR top_score IS NULL OR top_score < #{minScore}) " +
            "GROUP BY question ORDER BY ask_count DESC, last_ask_time DESC LIMIT #{limit}")
    List<Map<String, Object>> selectUnresolved(@Param("minScore") double minScore,
                                               @Param("limit") int limit);
}

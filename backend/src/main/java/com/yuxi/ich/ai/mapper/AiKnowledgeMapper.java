package com.yuxi.ich.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuxi.ich.ai.entity.AiKnowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 知识库文档表 Mapper。
 */
@Mapper
public interface AiKnowledgeMapper extends BaseMapper<AiKnowledge> {

    /**
     * 关键词检索知识文档：标题命中权重最高，其次内容命中。
     *
     * <p>简化实现：用 LIKE 匹配 + 分值排序模拟 BM25 的关键词检索效果，
     * 向量检索接入后可替换为混合检索（说明说 4.7.2 节）。
     *
     * @param keyword 用户问题关键词
     * @param limit   返回条数
     */
    @Select("<script>" +
            "SELECT * FROM ai_knowledge k WHERE k.deleted = 0 AND k.index_status = 1 " +
            "<if test='categoryId != null'> AND k.category_id = #{categoryId}</if>" +
            "<if test='region != null and region != \"\"'> AND k.region LIKE CONCAT('%', #{region}, '%')</if>" +
            " AND (" +
            "   k.title LIKE CONCAT('%', #{keyword}, '%')" +
            "   OR k.content LIKE CONCAT('%', #{keyword}, '%')" +
            " )" +
            " ORDER BY" +
            "   (CASE WHEN k.title LIKE CONCAT('%', #{keyword}, '%') THEN 2 ELSE 0 END" +
            "    + CASE WHEN k.content LIKE CONCAT('%', #{keyword}, '%') THEN 1 ELSE 0 END) DESC," +
            "   k.id ASC" +
            " LIMIT #{limit}" +
            "</script>")
    List<AiKnowledge> searchByKeyword(@Param("keyword") String keyword,
                                     @Param("categoryId") Long categoryId,
                                     @Param("region") String region,
                                     @Param("limit") int limit);
}

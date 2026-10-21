package com.yuxi.ich.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.ai.config.AiProperties;
import com.yuxi.ich.ai.dto.AiChatRequest;
import com.yuxi.ich.ai.dto.AiChatResponse;
import com.yuxi.ich.ai.dto.KnowledgeSaveRequest;
import com.yuxi.ich.ai.dto.RefItem;
import com.yuxi.ich.ai.entity.AiChat;
import com.yuxi.ich.ai.entity.AiKnowledge;
import com.yuxi.ich.ai.entity.AiKnowledgeChunk;
import com.yuxi.ich.ai.mapper.AiChatMapper;
import com.yuxi.ich.ai.mapper.AiKnowledgeChunkMapper;
import com.yuxi.ich.ai.mapper.AiKnowledgeMapper;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 智能问答服务（说明书 4.7 / 表 6-5 接口 43—53）。
 *
 * <p>流程：问题预处理 → 关键词检索知识库 → 打分排序 → 拼装引用
 * → 生成回答（可选大模型 / 离线模板）→ 落库 → 返回。
 * 知识库未命中时不编造内容，并计入「无结果问题清单」供管理员补充。
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    /** AI 回答最大字符数（utf8mb4 TEXT 为 65535 字节，留余量取 8000 字；超长直接拒绝落库，不截断） */
    static final int MAX_ANSWER_LENGTH = 8000;

    /**
     * 校验模型回答长度：超过上限抛 {@link BizException}，不做静默截断，避免写入半截或超长记录。
     * 包级可见以便单元测试直接覆盖。
     */
    static void assertAnswerLength(String answer) {
        if (answer != null && answer.length() > MAX_ANSWER_LENGTH) {
            throw new BizException(ResultCode.PARAM_ERROR, "AI 回答内容过长，未能保存");
        }
    }

    private static final List<String> STOP_WORDS = List.of(
            "什么是", "什么", "怎么样", "如何", "怎样", "请问", "介绍", "一下",
            "有哪些", "有什么", "吗", "呢", "的", "了", "和", "与", "以及",
            "关于", "聊聊", "说说", "讲解", "平台", "请", "我", "你", "它");

    private final AiKnowledgeMapper knowledgeMapper;
    private final AiKnowledgeChunkMapper chunkMapper;
    private final AiChatMapper chatMapper;
    private final AiProperties properties;
    private final AiRateLimiter rateLimiter;
    private final ModelCaller modelCaller;
    private final HttpServletRequest request;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiService(AiKnowledgeMapper knowledgeMapper, AiKnowledgeChunkMapper chunkMapper,
                     AiChatMapper chatMapper, AiProperties properties,
                     AiRateLimiter rateLimiter, ModelCaller modelCaller,
                     HttpServletRequest request) {
        this.knowledgeMapper = knowledgeMapper;
        this.chunkMapper = chunkMapper;
        this.chatMapper = chatMapper;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.modelCaller = modelCaller;
        this.request = request;
    }

    // ==================== 前台问答（接口 43） ====================

    @Transactional(rollbackFor = Exception.class)
    public AiChatResponse chat(AiChatRequest req) {
        // 游客限流
        Long userId = SecurityUtils.getUserIdOrNull();
        if (userId == null) {
            rateLimiter.check(clientIp(), properties.getGuestLimitPerMinute());
        } else {
            rateLimiter.check("u" + userId, properties.getGuestLimitPerMinute() * 5);
        }

        long start = System.currentTimeMillis();
        String sessionId = req.getSessionId() == null || req.getSessionId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "") : req.getSessionId();

        // 1. 关键词检索
        List<String> keywords = splitKeywords(req.getQuestion());
        Map<Long, AiKnowledge> hits = new LinkedHashMap<>();
        for (String kw : keywords) {
            List<AiKnowledge> found = knowledgeMapper.searchByKeyword(
                    kw, req.getCategoryId(), req.getRegion(), properties.getTopK() * 2);
            for (AiKnowledge k : found) {
                hits.putIfAbsent(k.getId(), k);
            }
            if (hits.size() >= properties.getTopK() * 3) {
                break;
            }
        }
        List<AiKnowledge> ranked = new ArrayList<>(hits.values());
        ranked.sort(Comparator.comparing(k -> -scoreOf(k, req.getQuestion())));
        if (ranked.size() > properties.getTopK()) {
            ranked = ranked.subList(0, properties.getTopK());
        }

        // 2. 引用与回答
        List<RefItem> refs = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            AiKnowledge k = ranked.get(i);
            refs.add(new RefItem(k.getTitle(), k.getSourceType(), k.getSourceId(),
                    frontUrlOf(k.getSourceType(), k.getSourceId()),
                    snippetOf(k.getContent())));
        }
        AiChatResponse resp = new AiChatResponse();
        resp.setSessionId(sessionId);
        resp.setRefs(refs);
        resp.setHit(!ranked.isEmpty());

        String answer;
        double topScore = 0d;
        if (ranked.isEmpty()) {
            answer = "很抱歉，平台非遗知识库暂未收录与该问题直接相关的资料。" +
                    "你可以尝试换个问法，或浏览「非遗项目」「传承人名录」栏目查找相关信息。";
        } else {
            topScore = scoreOf(ranked.get(0), req.getQuestion());
            if (properties.isEnabled()) {
                String context = buildContext(ranked);
                String modelAnswer = modelCaller.chat(context, req.getQuestion());
                answer = (modelAnswer == null || modelAnswer.isBlank())
                        ? buildTemplateAnswer(ranked) : modelAnswer;
            } else {
                answer = buildTemplateAnswer(ranked);
            }
        }

        // 3. 落库
        AiChat record = new AiChat();
        record.setUserId(userId);
        record.setSessionId(sessionId);
        record.setQuestion(req.getQuestion());
        assertAnswerLength(answer);
        record.setAnswer(answer);
        record.setRefs(writeRefs(refs));
        record.setTopScore(BigDecimal.valueOf(topScore));
        record.setHitCount(refs.size());
        record.setReviewStatus(0);
        record.setCostMs((int) (System.currentTimeMillis() - start));
        chatMapper.insert(record);

        resp.setChatId(record.getId());
        resp.setAnswer(answer);
        return resp;
    }

    /** 问答历史（登录，接口 44） */
    public PageResult<AiChat> history(long pageNum, long pageSize) {
        Long userId = SecurityUtils.getUserId();
        long offset = (pageNum - 1) * pageSize;
        List<AiChat> records = chatMapper.selectList(
                Wrappers.<AiChat>lambdaQuery()
                        .eq(AiChat::getUserId, userId)
                        .orderByDesc(AiChat::getCreateTime)
                        .last("LIMIT " + offset + ", " + pageSize));
        long total = chatMapper.selectCount(
                Wrappers.<AiChat>lambdaQuery().eq(AiChat::getUserId, userId));
        return new PageResult<>(total, records, pageNum, pageSize);
    }

    /** 引用来源（接口 46，公开） */
    public List<RefItem> refs(Long chatId) {
        AiChat chat = chatMapper.selectById(chatId);
        if (chat == null) {
            throw new BizException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        return readRefs(chat.getRefs());
    }

    /** 用户反馈（接口 45） */
    @Transactional(rollbackFor = Exception.class)
    public void feedback(Long chatId, Boolean useful) {
        if (useful == null) {
            throw new BizException(ResultCode.PARAM_ERROR, "缺少反馈值");
        }
        AiChat exist = chatMapper.selectById(chatId);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        Long userId = SecurityUtils.getUserIdOrNull();
        if (exist.getUserId() != null && (userId == null || !userId.equals(exist.getUserId()))) {
            throw new BizException(ResultCode.FORBIDDEN, "无权评价他人的问答");
        }
        AiChat update = new AiChat();
        update.setId(chatId);
        update.setIsUseful(useful ? 1 : 0);
        chatMapper.updateById(update);
    }

    // ==================== 管理端：知识库（接口 47—52） ====================

    /** 知识文档分页 */
    public PageResult<AiKnowledge> pageKnowledge(String keyword, Integer sourceType,
                                                 long pageNum, long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        var query = Wrappers.<AiKnowledge>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(),
                        AiKnowledge::getTitle, keyword)
                .eq(sourceType != null, AiKnowledge::getSourceType, sourceType)
                .orderByDesc(AiKnowledge::getCreateTime)
                .last("LIMIT " + offset + ", " + pageSize);
        long total = knowledgeMapper.selectCount(
                Wrappers.<AiKnowledge>lambdaQuery()
                        .like(keyword != null && !keyword.isBlank(),
                                AiKnowledge::getTitle, keyword)
                        .eq(sourceType != null, AiKnowledge::getSourceType, sourceType));
        return new PageResult<>(total, knowledgeMapper.selectList(query), pageNum, pageSize);
    }

    /** 新增知识文档（自动分块并置为已索引） */
    @Transactional(rollbackFor = Exception.class)
    public Long createKnowledge(KnowledgeSaveRequest req) {
        AiKnowledge doc = new AiKnowledge();
        doc.setTitle(req.getTitle());
        doc.setSourceType(req.getSourceType());
        doc.setSourceId(req.getSourceId());
        doc.setCategoryId(req.getCategoryId());
        doc.setRegion(req.getRegion());
        doc.setContent(req.getContent());
        doc.setIndexStatus(1);
        knowledgeMapper.insert(doc);
        reindexChunks(doc.getId());
        return doc.getId();
    }

    /** 更新知识文档（内容变更后重新分块） */
    @Transactional(rollbackFor = Exception.class)
    public void updateKnowledge(Long id, KnowledgeSaveRequest req) {
        AiKnowledge exist = knowledgeMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识文档不存在");
        }
        AiKnowledge doc = new AiKnowledge();
        doc.setId(id);
        doc.setTitle(req.getTitle());
        doc.setSourceType(req.getSourceType());
        doc.setSourceId(req.getSourceId());
        doc.setCategoryId(req.getCategoryId());
        doc.setRegion(req.getRegion());
        doc.setContent(req.getContent());
        knowledgeMapper.updateById(doc);
        reindexChunks(id);
    }

    /** 删除知识文档（逻辑删除 + 物理删切片） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledge(Long id) {
        AiKnowledge exist = knowledgeMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "知识文档不存在");
        }
        knowledgeMapper.deleteById(id);
        chunkMapper.delete(Wrappers.<AiKnowledgeChunk>lambdaQuery()
                .eq(AiKnowledgeChunk::getKnowledgeId, id));
    }

    /** 全量重建索引（接口 52：重新分块全部已审核文档） */
    @Transactional(rollbackFor = Exception.class)
    public int reindexAll() {
        List<AiKnowledge> docs = knowledgeMapper.selectList(
                Wrappers.<AiKnowledge>lambdaQuery().eq(AiKnowledge::getIndexStatus, 1));
        for (AiKnowledge doc : docs) {
            reindexChunks(doc.getId());
        }
        log.info("AI 知识库全量重建完成，处理 {} 篇文档", docs.size());
        return docs.size();
    }

    // ==================== 管理端：问答日志（接口 53） ====================

    /** 问答日志分页 */
    public PageResult<AiChat> chatLogs(String keyword, Integer reviewStatus,
                                       long pageNum, long pageSize) {
        long offset = (pageNum - 1) * pageSize;
        var query = Wrappers.<AiChat>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), AiChat::getQuestion, keyword)
                .eq(reviewStatus != null, AiChat::getReviewStatus, reviewStatus)
                .orderByDesc(AiChat::getCreateTime)
                .last("LIMIT " + offset + ", " + pageSize);
        var countQuery = Wrappers.<AiChat>lambdaQuery()
                .like(keyword != null && !keyword.isBlank(), AiChat::getQuestion, keyword)
                .eq(reviewStatus != null, AiChat::getReviewStatus, reviewStatus);
        return new PageResult<>(chatMapper.selectCount(countQuery),
                chatMapper.selectList(query), pageNum, pageSize);
    }

    /** 复核问答记录 */
    @Transactional(rollbackFor = Exception.class)
    public void reviewChat(Long id, Integer reviewStatus) {
        if (reviewStatus == null || (reviewStatus != 1 && reviewStatus != 2)) {
            throw new BizException(ResultCode.PARAM_ERROR, "复核状态不合法");
        }
        AiChat exist = chatMapper.selectById(id);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND, "问答记录不存在");
        }
        AiChat update = new AiChat();
        update.setId(id);
        update.setReviewStatus(reviewStatus);
        chatMapper.updateById(update);
    }

    /** 无结果问题清单（供管理员补知识库） */
    public List<Map<String, Object>> unresolvedQuestions(int limit) {
        return chatMapper.selectUnresolved(properties.getMinScore(), limit);
    }

    // ==================== 内部方法 ====================

    /**
     * 简单关键词切分。
     *
     * <p>步骤：按标点 / 空格切分 → 过滤停用词 → 追加「剥离尾部虚词」候选。
     * 例：「玉溪青花瓷是什么」经停用词过滤后为「玉溪青花瓷是」，
     * 与文档标题「玉溪青花瓷烧制技艺」无法 LIKE 连续匹配，故再剥离尾部
     * 「是/的/了/吗/呢/啊」生成「玉溪青花瓷」，保证召回。
     */
    private List<String> splitKeywords(String question) {
        String text = question == null ? "" : question.replaceAll("[，。！？、；：,.!?;:\\s]+", " ");
        List<String> result = new ArrayList<>();
        for (String token : text.split(" ")) {
            if (token == null || token.isBlank() || token.length() < 2) {
                continue;
            }
            String cleaned = token;
            for (String stop : STOP_WORDS) {
                cleaned = cleaned.replace(stop, "");
            }
            if (cleaned.isBlank()) {
                continue;
            }
            if (cleaned.length() > 30) {
                cleaned = cleaned.substring(0, 30);
            }
            addKeyword(result, cleaned);
            // 候选：剥离尾部虚词后更利于 LIKE 命中
            String trimmed = cleaned;
            while (trimmed.length() > 2
                    && "是的了吗呢啊".indexOf(trimmed.charAt(trimmed.length() - 1)) >= 0) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            if (trimmed.length() >= 2 && !trimmed.equals(cleaned)) {
                addKeyword(result, trimmed);
            }
        }
        if (result.isEmpty()) {
            addKeyword(result, text.length() > 30 ? text.substring(0, 30) : text);
        }
        return result;
    }

    private void addKeyword(List<String> list, String keyword) {
        if (!list.contains(keyword)) {
            list.add(keyword);
        }
    }

    /** 检索评分（标题命中 2 分、内容命中 1 分，归一化到 0-1） */
    private double scoreOf(AiKnowledge k, String question) {
        double s = 0d;
        if (question == null) {
            return s;
        }
        if (k.getTitle() != null && question.contains(k.getTitle())) {
            s += 2;
        } else if (k.getTitle() != null
                && k.getTitle().substring(0, Math.min(4, k.getTitle().length())).length() > 0
                && question.contains(k.getTitle().substring(0, Math.min(4, k.getTitle().length())))) {
            s += 1.5;
        }
        if (k.getContent() != null && k.getContent().length() > 4
                && question.contains(k.getContent().substring(0, 4))) {
            s += 1;
        }
        return Math.min(1.0, s / 3.0);
    }

    /** 离线模板回答（含 [n] 引用角标，注明来源范围） */
    private String buildTemplateAnswer(List<AiKnowledge> ranked) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据平台非遗知识库的已审核资料，为你整理如下：\n\n");
        for (int i = 0; i < ranked.size(); i++) {
            AiKnowledge k = ranked.get(i);
            sb.append("[").append(i + 1).append("] ")
                    .append(k.getTitle()).append("\n")
                    .append(snippetOf(k.getContent())).append("\n\n");
        }
        sb.append("（以上内容来自平台非遗知识库，引用来源见下方卡片；")
                .append("知识库未覆盖的细节建议咨询当地文化和旅游部门。）");
        return sb.toString();
    }

    /** 拼装给大模型的上下文 */
    private String buildContext(List<AiKnowledge> ranked) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ranked.size(); i++) {
            AiKnowledge k = ranked.get(i);
            sb.append("[").append(i + 1).append("] ").append(k.getTitle()).append("\n")
                    .append(k.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /** 内容摘要（200 字内） */
    private String snippetOf(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String flat = content.replaceAll("\\s+", " ");
        return flat.length() <= 200 ? flat : flat.substring(0, 200) + "…";
    }

    /** 前端跳转路由 */
    private String frontUrlOf(Integer sourceType, Long sourceId) {
        if (sourceId == null) {
            return null;
        }
        return switch (sourceType) {
            case 1 -> "/project/" + sourceId;
            case 2 -> "/inheritor/" + sourceId;
            case 3 -> "/article/" + sourceId;
            default -> null;
        };
    }

    /** 重新分块：按 500 字切分（重叠 50 字），并回填 chunk_count */
    private void reindexChunks(Long knowledgeId) {
        AiKnowledge doc = knowledgeMapper.selectById(knowledgeId);
        if (doc == null) {
            return;
        }
        chunkMapper.delete(Wrappers.<AiKnowledgeChunk>lambdaQuery()
                .eq(AiKnowledgeChunk::getKnowledgeId, knowledgeId));
        String content = doc.getContent();
        if (content == null || content.isBlank()) {
            updateChunkCount(knowledgeId, 0);
            return;
        }
        int chunkSize = 500;
        int overlap = 50;
        List<AiKnowledgeChunk> chunks = new ArrayList<>();
        int index = 0;
        int pos = 0;
        while (pos < content.length()) {
            int end = Math.min(pos + chunkSize, content.length());
            AiKnowledgeChunk chunk = new AiKnowledgeChunk();
            chunk.setKnowledgeId(knowledgeId);
            chunk.setChunkIndex(index++);
            chunk.setContent(content.substring(pos, end));
            chunks.add(chunk);
            if (end >= content.length()) {
                break;
            }
            pos = end - overlap;
        }
        if (chunks.isEmpty()) {
            updateChunkCount(knowledgeId, 0);
            return;
        }
        for (AiKnowledgeChunk c : chunks) {
            chunkMapper.insert(c);
        }
        updateChunkCount(knowledgeId, chunks.size());
    }

    private void updateChunkCount(Long knowledgeId, int count) {
        AiKnowledge update = new AiKnowledge();
        update.setId(knowledgeId);
        update.setChunkCount(count);
        update.setIndexStatus(1);
        knowledgeMapper.updateById(update);
    }

    private String writeRefs(List<RefItem> refs) {
        try {
            return objectMapper.writeValueAsString(refs);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<RefItem> readRefs(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<RefItem>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

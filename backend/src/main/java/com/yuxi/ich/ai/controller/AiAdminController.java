package com.yuxi.ich.ai.controller;

import com.yuxi.ich.ai.dto.KnowledgeSaveRequest;
import com.yuxi.ich.ai.entity.AiChat;
import com.yuxi.ich.ai.entity.AiKnowledge;
import com.yuxi.ich.ai.service.AiService;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 知识库管理接口（对应说明书表 6-5 接口 47—53，管理员）。
 */
@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AiAdminController {

    private final AiService aiService;

    public AiAdminController(AiService aiService) {
        this.aiService = aiService;
    }

    /** 知识文档分页（接口 47） */
    @GetMapping("/knowledge")
    public Result<PageResult<AiKnowledge>> knowledge(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sourceType", required = false) Integer sourceType,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(aiService.pageKnowledge(keyword, sourceType, pageNum, pageSize));
    }

    /** 新增知识文档（接口 48） */
    @PostMapping("/knowledge")
    public Result<Long> createKnowledge(@Valid @RequestBody KnowledgeSaveRequest request) {
        return Result.success("创建成功", aiService.createKnowledge(request));
    }

    /** 更新知识文档（接口 49） */
    @PutMapping("/knowledge/{id}")
    public Result<Void> updateKnowledge(@PathVariable("id") Long id,
                                        @Valid @RequestBody KnowledgeSaveRequest request) {
        aiService.updateKnowledge(id, request);
        return Result.success("更新成功", null);
    }

    /** 删除知识文档（接口 50） */
    @DeleteMapping("/knowledge/{id}")
    public Result<Void> deleteKnowledge(@PathVariable("id") Long id) {
        aiService.deleteKnowledge(id);
        return Result.success("删除成功", null);
    }

    /** 全量重建索引（接口 52） */
    @PostMapping("/reindex")
    public Result<Map<String, Object>> reindex() {
        int count = aiService.reindexAll();
        return Result.success("重建完成", Map.of("processed", count));
    }

    /** 问答日志分页（接口 53） */
    @GetMapping("/chat/logs")
    public Result<PageResult<AiChat>> chatLogs(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "reviewStatus", required = false) Integer reviewStatus,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(aiService.chatLogs(keyword, reviewStatus, pageNum, pageSize));
    }

    /** 复核问答记录 */
    @PutMapping("/chat/{id}/review")
    public Result<Void> review(@PathVariable("id") Long id,
                               @RequestBody Map<String, Integer> body) {
        aiService.reviewChat(id, body.get("reviewStatus"));
        return Result.success("复核完成", null);
    }

    /** 无结果问题清单（接口 53 附） */
    @GetMapping("/chat/unresolved")
    public Result<List<Map<String, Object>>> unresolved(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Result.success(aiService.unresolvedQuestions(limit));
    }
}

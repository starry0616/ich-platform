package com.yuxi.ich.ai.controller;

import com.yuxi.ich.ai.dto.AiChatRequest;
import com.yuxi.ich.ai.dto.AiChatResponse;
import com.yuxi.ich.ai.dto.RefItem;
import com.yuxi.ich.ai.entity.AiChat;
import com.yuxi.ich.ai.service.AiService;
import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 智能问答前台接口（对应说明书表 6-5 接口 43—46）。
 *
 * <p>访问控制：提问与来源查看公开（游客可体验），
 * 历史记录与反馈需登录；游客提问受限流保护（TC-12）。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    /** 智能问答（公开，游客限流） */
    @PostMapping("/chat")
    public Result<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return Result.success(aiService.chat(request));
    }

    /** 我的问答历史（登录） */
    @GetMapping("/chat/history")
    public Result<PageResult<AiChat>> history(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(aiService.history(pageNum, pageSize));
    }

    /** 回答反馈（登录） */
    @PostMapping("/chat/{id}/feedback")
    public Result<Void> feedback(@PathVariable("id") Long id,
                                 @RequestBody Map<String, Boolean> body) {
        aiService.feedback(id, body.get("useful"));
        return Result.success("反馈已记录，感谢你的参与", null);
    }

    /** 引用来源（公开） */
    @GetMapping("/chat/{id}/refs")
    public Result<List<RefItem>> refs(@PathVariable("id") Long id) {
        return Result.success(aiService.refs(id));
    }
}

package com.yuxi.ich.interaction.controller;

import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.interaction.dto.InteractionRequest;
import com.yuxi.ich.interaction.entity.Interaction;
import com.yuxi.ich.interaction.service.InteractionService;
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户互动接口（对应说明书表 6-4 接口 38—42）。
 *
 * <p>收藏与点赞是开关式操作，返回体里带上操作后的状态，
 * 前端据此直接更新按钮，无需再发一次查询。
 */
@RestController
@RequestMapping("/api/interaction")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    /**
     * 切换收藏。
     *
     * <pre>
     * POST /api/interaction/favorite?targetType=1&amp;targetId=1
     * → { "code":200, "data": { "active": true } }   true 表示已收藏
     * </pre>
     */
    @PostMapping("/favorite")
    public Result<Map<String, Object>> favorite(
            @RequestParam("targetType") Integer targetType,
            @RequestParam("targetId") Long targetId) {
        boolean active = interactionService.toggleFavorite(targetType, targetId);
        return Result.success(active ? "收藏成功" : "已取消收藏", state(active));
    }

    /** 切换点赞 */
    @PostMapping("/like")
    public Result<Map<String, Object>> like(
            @RequestParam("targetType") Integer targetType,
            @RequestParam("targetId") Long targetId) {
        boolean active = interactionService.toggleLike(targetType, targetId);
        return Result.success(active ? "点赞成功" : "已取消点赞", state(active));
    }

    /**
     * 查询当前登录用户对某目标的互动状态，用于按钮回显。
     *
     * <pre>
     * GET /api/interaction/state?targetType=1&amp;targetId=1
     * → { "favorited": true, "liked": false }
     * </pre>
     */
    @GetMapping("/state")
    public Result<Map<String, Object>> state(
            @RequestParam("targetType") Integer targetType,
            @RequestParam("targetId") Long targetId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favorited", interactionService.hasInteracted(
                targetType, targetId, 1));
        data.put("liked", interactionService.hasInteracted(
                targetType, targetId, 2));
        return Result.success(data);
    }

    /**
     * 发表评论或回复。
     *
     * <pre>
     * POST /api/interaction/comment
     * { "targetType":1, "targetId":1, "content":"很精彩", "parentId":0 }
     * </pre>
     */
    @PostMapping("/comment")
    public Result<Long> comment(@Valid @RequestBody InteractionRequest request) {
        request.setType(3);
        return Result.success("评论成功", interactionService.comment(request));
    }

    /**
     * 评论列表（公开接口，无需登录）。
     *
     * <pre>
     * GET /api/interaction/comments?targetType=1&amp;targetId=1&amp;pageNum=1&amp;pageSize=10
     * </pre>
     */
    @GetMapping("/comments")
    public Result<PageResult<Interaction>> comments(InteractionRequest.CommentQuery query) {
        return Result.success(interactionService.comments(query.getTargetType(),
                query.getTargetId(), query.getPageNum(), query.getPageSize()));
    }

    /**
     * 删除互动记录（取消收藏 / 取消点赞 / 删除自己的评论）。
     *
     * <p>只能删自己的记录，管理员例外；越权返回 403。
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable("id") Long id) {
        interactionService.remove(id);
        return Result.success("删除成功", null);
    }

    /** 我的收藏列表（跨项目与资讯） */
    @GetMapping("/my/favorites")
    public Result<PageResult<Interaction>> myFavorites(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(interactionService.myFavorites(pageNum, pageSize));
    }

    // ==================== 后台治理 ====================

    /** 屏蔽 / 恢复评论（管理员） */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> setStatus(@PathVariable("id") Long id,
                                  @RequestParam("status") Integer status) {
        interactionService.setCommentStatus(id, status);
        return Result.success("操作成功", null);
    }

    private Map<String, Object> state(boolean active) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("active", active);
        return data;
    }
}

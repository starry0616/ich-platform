package com.yuxi.ich.message.controller;

import com.yuxi.ich.common.response.PageResult;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.message.entity.SysMessage;
import com.yuxi.ich.message.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 消息通知接口（对应说明书表 6-4 接口 42）。
 */
@RestController
@RequestMapping("/api/message")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /** 我的消息分页（登录） */
    @GetMapping("/list")
    public Result<PageResult<SysMessage>> list(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return Result.success(messageService.page(pageNum, pageSize));
    }

    /** 未读消息数（登录） */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> unreadCount() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unread", messageService.unreadCount());
        return Result.success(data);
    }

    /** 标记单条已读 */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id) {
        messageService.markRead(id);
        return Result.success("已读", null);
    }

    /** 全部标为已读 */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead();
        return Result.success("全部已读", null);
    }
}

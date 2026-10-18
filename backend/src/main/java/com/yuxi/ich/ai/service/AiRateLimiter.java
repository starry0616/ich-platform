package com.yuxi.ich.ai.service;

import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.ResultCode;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 问答游客限流（内存滑动窗口，单实例有效）。
 *
 * <p>设计文档 TC-12：未登录用户短时间连续提问超过配额返回 429。
 * 生产多实例部署时应替换为 Redis 计数器；登录用户可按角色放宽配额。
 */
@Component
public class AiRateLimiter {

    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();

    /**
     * 尝试放行一次请求。
     *
     * @param key 限流键（游客用 IP，登录用户用 userId 前缀）
     * @param limitPerMinute 每分钟上限
     */
    public void check(String key, int limitPerMinute) {
        if (key == null || limitPerMinute <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > 60_000L) {
                deque.pollFirst();
            }
            if (deque.size() >= limitPerMinute) {
                throw new BizException(ResultCode.TOO_MANY_REQUESTS,
                        "提问过于频繁，请稍后再试");
            }
            deque.addLast(now);
        }
    }
}

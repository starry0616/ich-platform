package com.yuxi.ich.security;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 令牌黑名单。
 *
 * <p>用于实现「退出登录后令牌立即失效」。JWT 本身无状态，
 * 签发后在有效期内始终可用，因此需要一份黑名单来吊销。
 *
 * <p><b>骨架阶段实现</b>：使用进程内 ConcurrentHashMap 并带过期清理，
 * 保证不依赖 Redis 即可运行与测试。
 *
 * <p><b>接入 Redis 的方式</b>：把本类替换为 Redis 实现即可，接口不变：
 * <pre>
 * // 写入：SETEX blacklist:{token} {剩余秒数} 1
 * // 查询：EXISTS blacklist:{token}
 * </pre>
 * 这样多实例部署时黑名单才能共享。替换时只需改本类，业务代码无需改动。
 */
@Component
public class TokenBlacklist {

    /** token -> 过期时间戳（毫秒） */
    private final Map<String, Long> store = new ConcurrentHashMap<>();

    /**
     * 将令牌加入黑名单。
     *
     * @param token         令牌原文
     * @param ttlMillis     剩余有效时长；到点后自动清理
     */
    public void add(String token, long ttlMillis) {
        if (token == null || token.isBlank()) {
            return;
        }
        long expireAt = System.currentTimeMillis() + Math.max(ttlMillis, 0);
        store.put(token, expireAt);
        cleanExpired();
    }

    /** 判断令牌是否已被吊销 */
    public boolean contains(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Long expireAt = store.get(token);
        if (expireAt == null) {
            return false;
        }
        if (expireAt < System.currentTimeMillis()) {
            store.remove(token);
            return false;
        }
        return true;
    }

    /** 清理已过期条目，避免长期运行内存增长 */
    private void cleanExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> e.getValue() < now);
    }

    /**
     * 清空黑名单。
     *
     * <p>仅供测试重置共享状态使用。生产环境不应调用——
     * 清空会让已退出的令牌重新可用。
     */
    public void clear() {
        store.clear();
    }

    /** 当前黑名单规模（测试与监控用） */
    public int size() {
        cleanExpired();
        return store.size();
    }
}

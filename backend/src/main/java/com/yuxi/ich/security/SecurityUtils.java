package com.yuxi.ich.security;

import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类。
 *
 * <p>业务代码取当前用户只走这里，避免各处重复解析 SecurityContext：
 * <pre>
 * Long userId = SecurityUtils.getUserId();          // 已登录时返回 id
 * Long uid = SecurityUtils.getUserIdOrNull();       // 游客场景返回 null
 * </pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** 获取当前登录用户，未登录返回 null */
    public static LoginUser getLoginUserOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /** 获取当前登录用户，未登录抛 401 */
    public static LoginUser getLoginUser() {
        LoginUser user = getLoginUserOrNull();
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    /** 获取当前用户 id，未登录抛 401 */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /** 获取当前用户 id，未登录返回 null（用于可选登录的接口，如 AI 问答） */
    public static Long getUserIdOrNull() {
        LoginUser user = getLoginUserOrNull();
        return user == null ? null : user.getUserId();
    }

    /** 是否管理员 */
    public static boolean isAdmin() {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.isAdmin();
    }

    /**
     * 校验数据归属：非管理员只能操作自己的数据。
     *
     * <p>用于「按 id 修改/删除」类接口的越权防护——不校验的话，
     * 用户篡改 id 就能改别人的数据。
     *
     * @param ownerUserId 数据所属用户 id
     */
    public static void checkOwnership(Long ownerUserId) {
        if (isAdmin()) {
            return;
        }
        Long current = getUserId();
        if (ownerUserId == null || !ownerUserId.equals(current)) {
            throw new BizException(ResultCode.FORBIDDEN, "无权操作他人数据");
        }
    }
}

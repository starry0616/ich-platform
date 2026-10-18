package com.yuxi.ich.common.constant;

/**
 * 角色编码常量。
 *
 * <p>与数据库 sys_role.role_code 保持一致，共三类角色。
 * 后端鉴权使用 {@code @PreAuthorize("hasRole('ADMIN')")}，
 * 注意 Spring Security 的 hasRole 会自动补 "ROLE_" 前缀。
 */
public final class RoleConstants {

    private RoleConstants() {
    }

    /** 普通用户：注册公众 */
    public static final String USER = "ROLE_USER";
    /** 非遗传承人：经认证的内容生产者 */
    public static final String INHERITOR = "ROLE_INHERITOR";
    /** 平台管理员：运营与审核 */
    public static final String ADMIN = "ROLE_ADMIN";
}

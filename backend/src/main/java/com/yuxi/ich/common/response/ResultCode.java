package com.yuxi.ich.common.response;

/**
 * 统一业务状态码。
 *
 * <p>与《系统设计说明书》表 6-1 保持一致，前后端共同约定：
 * 200 成功、400 参数错误、401 未认证、403 无权限、404 不存在、
 * 409 状态冲突（重复报名、名额已满等）、429 限流、500 服务端异常。
 */
public enum ResultCode {

    SUCCESS(200, "操作成功"),

    PARAM_ERROR(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "操作冲突，请刷新后重试"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    ERROR(500, "服务器内部错误");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}

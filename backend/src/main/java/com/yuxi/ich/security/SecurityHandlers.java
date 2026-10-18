package com.yuxi.ich.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.common.response.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 认证与鉴权失败处理。
 *
 * <p>Spring Security 默认返回 HTML 或空响应体，前后端分离下必须改成 JSON，
 * 否则前端拿到 401/403 却读不到原因。
 */
public final class SecurityHandlers {

    private SecurityHandlers() {
    }

    private static void write(HttpServletResponse response, ResultCode code, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Result<Void> body = Result.fail(code, message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
    }

    /** 未认证：返回 401 */
    @Component
    public static class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response,
                             AuthenticationException authException) throws IOException {
            write(response, ResultCode.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
        }
    }

    /** 已认证但权限不足：返回 403 */
    @Component
    public static class JsonAccessDeniedHandler implements AccessDeniedHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                           AccessDeniedException accessDeniedException) throws IOException {
            write(response, ResultCode.FORBIDDEN, "没有操作权限");
        }
    }
}

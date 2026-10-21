package com.yuxi.ich.auth.controller;

import com.yuxi.ich.auth.dto.LoginRequest;
import com.yuxi.ich.auth.dto.LoginResponse;
import com.yuxi.ich.auth.dto.RegisterRequest;
import com.yuxi.ich.auth.service.AuthService;
import com.yuxi.ich.common.response.Result;
import com.yuxi.ich.security.LoginUser;
import com.yuxi.ich.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 认证接口（对应说明书表 6-2 的接口 1—5）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 注册 */
    @PostMapping("/register")
    public Result<Long> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success("注册成功", authService.register(request));
    }

    /** 登录，返回双令牌与用户基本信息 */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success("登录成功", authService.login(request));
    }

    /** 刷新访问令牌 */
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(
            @RequestParam(value = "refreshToken", required = false) String refreshToken,
            @RequestHeader(value = "X-Refresh-Token", required = false) String headerToken) {
        String token = (refreshToken != null && !refreshToken.isBlank())
                ? refreshToken : headerToken;
        return Result.success(authService.refresh(token));
    }

    /**
     * 退出登录。
     *
     * <p>访问令牌取自 Authorization 头；刷新令牌可通过 query {@code refreshToken}
     * 或 {@code X-Refresh-Token} 头传入，二者均会被拉黑。令牌缺失不报错。
     */
    @PostMapping("/logout")
    public Result<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "refreshToken", required = false) String refreshParam,
            @RequestHeader(value = "X-Refresh-Token", required = false) String refreshHeader) {
        String refreshToken = (refreshParam != null && !refreshParam.isBlank())
                ? refreshParam : refreshHeader;
        authService.logout(authorization, refreshToken);
        return Result.success("已退出登录", null);
    }

    /**
     * 当前登录用户信息。
     *
     * <p>前端刷新页面时调用，用于恢复登录态。
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        LoginUser user = SecurityUtils.getLoginUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getUserId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("roleCode", user.getRoleCode());
        data.put("isAdmin", user.isAdmin());
        data.put("isInheritor", user.isInheritor());
        return Result.success(data);
    }
}

package com.yuxi.ich.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yuxi.ich.common.constant.RoleConstants;
import com.yuxi.ich.common.constant.StatusConstants;
import com.yuxi.ich.common.exception.BizException;
import com.yuxi.ich.common.response.ResultCode;
import com.yuxi.ich.auth.dto.LoginRequest;
import com.yuxi.ich.auth.dto.LoginResponse;
import com.yuxi.ich.auth.dto.RegisterRequest;
import com.yuxi.ich.config.JwtProperties;
import com.yuxi.ich.security.JwtTokenProvider;
import com.yuxi.ich.security.LoginUser;
import com.yuxi.ich.security.TokenBlacklist;
import com.yuxi.ich.user.entity.SysLoginLog;
import com.yuxi.ich.user.entity.SysRole;
import com.yuxi.ich.user.entity.SysUser;
import com.yuxi.ich.user.mapper.SysLoginLogMapper;
import com.yuxi.ich.user.mapper.SysRoleMapper;
import com.yuxi.ich.user.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 认证服务：注册、登录、刷新、退出。
 *
 * <p>安全要点：
 * <ul>
 *   <li>密码用 BCrypt 加盐哈希存储，不可逆</li>
 *   <li>登录失败统一提示「用户名或密码错误」，不区分是哪一个错，避免账号枚举</li>
 *   <li>被禁用的账号即使密码正确也拒绝登录</li>
 *   <li>退出时把令牌写入黑名单，使其在自然过期前即失效</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final TokenBlacklist tokenBlacklist;
    private final JwtProperties jwtProperties;
    private final SysLoginLogMapper loginLogMapper;
    private final HttpServletRequest request;

    public AuthService(SysUserMapper userMapper, SysRoleMapper roleMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
                       TokenBlacklist tokenBlacklist, JwtProperties jwtProperties,
                       SysLoginLogMapper loginLogMapper, HttpServletRequest request) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
        this.jwtProperties = jwtProperties;
        this.loginLogMapper = loginLogMapper;
        this.request = request;
    }

    /**
     * 用户注册。默认分配普通用户角色。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(RegisterRequest req) {
        Long exists = userMapper.selectCount(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(ResultCode.CONFLICT, "用户名已被占用");
        }

        SysUser user = new SysUser();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() == null || req.getNickname().isBlank()
                ? req.getUsername() : req.getNickname());
        user.setPhone(emptyToNull(req.getPhone()));
        user.setEmail(emptyToNull(req.getEmail()));
        user.setRoleId(resolveRoleId(RoleConstants.USER));
        user.setStatus(StatusConstants.User.NORMAL);
        userMapper.insert(user);

        log.info("新用户注册：id={} username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    /**
     * 登录。
     *
     * <p>注意：本方法不开启事务，登录日志立即落库——
     * 失败登录本身就是需要留痕的安全事件，不能随业务回滚丢失。
     */
    public LoginResponse login(LoginRequest req) {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, req.getUsername()));

        // 统一提示，不区分「用户不存在」与「密码错误」，防止账号枚举
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("登录失败：{}", req.getUsername());
            recordLoginLog(null, req.getUsername(), 0, "用户名或密码错误");
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == StatusConstants.User.DISABLED) {
            recordLoginLog(user.getId(), user.getUsername(), 0, "账号已被禁用");
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        SysRole role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? RoleConstants.USER : role.getRoleCode();

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(),
                user.getNickname(), user.getRoleId(), roleCode, user.getStatus());

        String accessToken = tokenProvider.createAccessToken(loginUser);
        String refreshToken = tokenProvider.createRefreshToken(loginUser);

        // 记录最近登录时间
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(update);

        LoginResponse resp = new LoginResponse(accessToken, refreshToken,
                jwtProperties.getAccessTokenMinutes() * 60);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setRoleCode(roleCode);
        resp.setAvatar(user.getAvatar());
        recordLoginLog(user.getId(), user.getUsername(), 1, "登录成功");
        return resp;
    }

    /** 记录登录日志（说明书 7.4 节：账号、IP、时间与结果） */
    private void recordLoginLog(Long userId, String username, int status, String message) {
        try {
            SysLoginLog log = new SysLoginLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setIp(clientIp());
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setStatus(status);
            log.setMessage(message);
            log.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception e) {
            // 登录日志不允许影响登录主流程
            log.warn("登录日志写入失败：{}", e.getMessage());
        }
    }

    private String clientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 用刷新令牌换取新的访问令牌。
     *
     * <p>刷新时会重新查库，确认账号未被禁用、角色未变更，
     * 避免「已禁用的账号靠旧令牌一直续期」。
     */
    public LoginResponse refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()
                || !tokenProvider.isValidRefreshToken(refreshToken)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        // 已退出登录的 refresh token 不得再换取新的访问令牌
        if (tokenBlacklist.contains(refreshToken)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "会话已退出，请重新登录");
        }
        LoginUser parsed = parseRefresh(refreshToken);
        if (parsed == null || parsed.getUserId() == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "刷新令牌无效或已过期");
        }

        SysUser user = userMapper.selectById(parsed.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号不存在");
        }
        if (user.getStatus() != null && user.getStatus() == StatusConstants.User.DISABLED) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
        SysRole role = user.getRoleId() == null ? null : roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? RoleConstants.USER : role.getRoleCode();

        LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(),
                user.getNickname(), user.getRoleId(), roleCode, user.getStatus());
        LoginResponse resp = new LoginResponse(
                tokenProvider.createAccessToken(loginUser),
                tokenProvider.createRefreshToken(loginUser),
                jwtProperties.getAccessTokenMinutes() * 60);
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setNickname(user.getNickname());
        resp.setRoleCode(roleCode);
        return resp;
    }

    /**
     * 退出登录：同时把访问令牌与刷新令牌加入黑名单。
     *
     * <p>访问令牌按其剩余有效期拉黑；刷新令牌若提供且合法，按其 7 天有效期拉黑，
     * 使其在退出后无法再换取新的访问令牌。refresh token 缺失或非法时静默跳过，
     * 兼容重复登出、本地无 refresh 令牌等情况，不抛错。
     *
     * <p>限制：黑名单为进程内内存实现（见 {@link TokenBlacklist}），应用重启后
     * 撤销记录丢失，不构成持久化会话管理；多实例部署需替换为 Redis 黑名单。
     */
    public void logout(String authorizationHeader, String refreshTokenRaw) {
        String accessToken = tokenProvider.resolveToken(authorizationHeader);
        if (accessToken != null) {
            // 黑名单保留到令牌自然过期即可
            tokenBlacklist.add(accessToken, jwtProperties.getAccessTokenMinutes() * 60 * 1000L);
        }
        if (refreshTokenRaw != null && !refreshTokenRaw.isBlank()) {
            String refreshToken = tokenProvider.resolveToken(refreshTokenRaw);
            if (tokenProvider.isValidRefreshToken(refreshToken)) {
                tokenBlacklist.add(refreshToken,
                        jwtProperties.getRefreshTokenDays() * 24 * 60 * 60 * 1000L);
            }
        }
    }

    /** 从刷新令牌中取出用户信息（载荷与访问令牌一致） */
    private LoginUser parseRefresh(String refreshToken) {
        io.jsonwebtoken.Claims claims = tokenProvider.parse(refreshToken);
        if (claims == null) {
            return null;
        }
        LoginUser user = new LoginUser();
        Object uid = claims.get("uid");
        user.setUserId(uid == null ? null : Long.valueOf(String.valueOf(uid)));
        user.setUsername(claims.get("uname", String.class));
        user.setRoleCode(claims.get("role", String.class));
        return user;
    }

    private Long resolveRoleId(String roleCode) {
        SysRole role = roleMapper.selectOne(
                Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, roleCode));
        if (role == null) {
            throw new BizException(ResultCode.ERROR, "角色未初始化：" + roleCode
                    + "，请先执行 00_schema.sql");
        }
        return role.getId();
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

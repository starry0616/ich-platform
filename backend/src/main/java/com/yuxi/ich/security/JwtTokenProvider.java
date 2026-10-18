package com.yuxi.ich.security;

import com.yuxi.ich.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 令牌生成与解析。
 *
 * <p>采用双令牌方案：
 * <ul>
 *   <li>access token —— 业务请求使用，有效期 2 小时</li>
 *   <li>refresh token —— 仅用于换取新的 access token，有效期 7 天</li>
 * </ul>
 * 令牌载荷中携带用户 id、用户名与角色，服务端无状态校验，不查库。
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(
                properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 生成访问令牌 */
    public String createAccessToken(LoginUser user) {
        return create(user, TYPE_ACCESS,
                properties.getAccessTokenMinutes() * 60 * 1000L);
    }

    /** 生成刷新令牌 */
    public String createRefreshToken(LoginUser user) {
        return create(user, TYPE_REFRESH,
                properties.getRefreshTokenDays() * 24 * 60 * 60 * 1000L);
    }

    private String create(LoginUser user, String type, long ttlMillis) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMillis);
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_USER_ID, user.getUserId());
        claims.put(CLAIM_USERNAME, user.getUsername());
        claims.put(CLAIM_ROLE, user.getRoleCode());
        claims.put(CLAIM_TYPE, type);
        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(user.getUserId()))
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /** 解析令牌，失败返回 null（不抛异常，由调用方决定如何处理） */
    public Claims parse(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("令牌解析失败：{}", e.getMessage());
            return null;
        }
    }

    /** 从访问令牌还原登录用户；令牌无效或类型不符返回 null */
    public LoginUser toLoginUser(String token) {
        Claims claims = parse(token);
        if (claims == null) {
            return null;
        }
        if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
            log.debug("令牌类型不是 access，拒绝用于业务请求");
            return null;
        }
        LoginUser user = new LoginUser();
        Object uid = claims.get(CLAIM_USER_ID);
        user.setUserId(uid == null ? null : Long.valueOf(String.valueOf(uid)));
        user.setUsername(claims.get(CLAIM_USERNAME, String.class));
        user.setRoleCode(claims.get(CLAIM_ROLE, String.class));
        return user;
    }

    /** 校验刷新令牌是否有效 */
    public boolean isValidRefreshToken(String token) {
        Claims claims = parse(token);
        return claims != null && TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** 从请求头原始值中剥离前缀，得到纯令牌 */
    public String resolveToken(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return null;
        }
        String prefix = properties.getPrefix();
        if (prefix != null && !prefix.isBlank() && headerValue.startsWith(prefix)) {
            return headerValue.substring(prefix.length()).trim();
        }
        return headerValue.trim();
    }

    public String getHeaderName() {
        return properties.getHeader();
    }
}

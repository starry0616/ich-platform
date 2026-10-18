package com.yuxi.ich.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 相关配置，对应 application.yml 中的 ich.jwt.*。
 */
@Component
@ConfigurationProperties(prefix = "ich.jwt")
public class JwtProperties {

    /** 签名密钥，生产环境必须通过环境变量覆盖 */
    private String secret = "ich-platform-dev-secret-key-please-change-in-production-0123456789";
    /** 访问令牌有效期（分钟） */
    private long accessTokenMinutes = 120;
    /** 刷新令牌有效期（天） */
    private long refreshTokenDays = 7;
    /** 令牌所在请求头名称 */
    private String header = "Authorization";
    /** 令牌前缀 */
    private String prefix = "Bearer ";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}

package com.yuxi.ich;

import com.yuxi.ich.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CORS 配置单元测试（第21批，纯逻辑，不起 Spring 上下文、不连外部）。
 *
 * <p>验证：未配置 ICH_CORS_ORIGINS 时回退本地开发来源；配置精确 Origin 时使用
 * 精确 allowedOrigins 而非通配，且 allowCredentials 保持开启。
 */
class CorsConfigTests {

    private CorsConfiguration resolve(String configuredOrigins) {
        SecurityConfig config = new SecurityConfig(null, null, null);
        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) config.buildCorsSource(configuredOrigins);
        return source.getCorsConfigurations().get("/**");
    }

    @Test
    @DisplayName("未配置来源时回退本地 localhost/127.0.0.1 通配（开发默认）")
    void fallbackToLocalPatternsWhenBlank() {
        CorsConfiguration c = resolve("");
        assertThat(c.getAllowedOriginPatterns())
                .containsExactlyInAnyOrder("http://localhost:*", "http://127.0.0.1:*");
        assertThat(c.getAllowedOrigins()).isNullOrEmpty();
        assertThat(c.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("配置精确 Origin 时使用 allowedOrigins，不使用通配来源")
    void usesExactOriginsWhenConfigured() {
        CorsConfiguration c = resolve("https://admin.example.com, https://www.example.com");
        assertThat(c.getAllowedOrigins())
                .isEqualTo(List.of("https://admin.example.com", "https://www.example.com"));
        assertThat(c.getAllowedOriginPatterns()).isNullOrEmpty();
        assertThat(c.getAllowCredentials()).isTrue();
    }

    @Test
    @DisplayName("配置仅含空白/逗号时仍回退本地默认，不产生空来源列表")
    void blankConfigFallsBackToLocal() {
        CorsConfiguration c = resolve(" , ");
        assertThat(c.getAllowedOriginPatterns())
                .containsExactlyInAnyOrder("http://localhost:*", "http://127.0.0.1:*");
    }
}

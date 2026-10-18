package com.yuxi.ich.config;

import com.yuxi.ich.security.JwtAuthenticationFilter;
import com.yuxi.ich.security.SecurityHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 配置。
 *
 * <p>核心约定：
 * <ul>
 *   <li>无状态：不创建 Session，身份完全依赖 JWT</li>
 *   <li>CSRF 关闭：令牌放在请求头而非 Cookie，天然不受 CSRF 影响</li>
 *   <li>公开接口显式放行，其余一律需要认证</li>
 *   <li>细粒度权限由 {@code @PreAuthorize} 在 Controller 上声明</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity   // 开启 @PreAuthorize
public class SecurityConfig {

    /**
     * 无需登录即可访问的路径。
     *
     * <p><b>踩过的坑</b>：Spring Security 的 Ant 风格通配符中，
     * 单个 {@code *} 只匹配<b>一层</b>路径段。例如 {@code /api/article/*}
     * 能匹配 {@code /api/article/1}，但<b>匹配不到</b> {@code /api/article/detail/1}，
     * 后者会落到 anyRequest().authenticated() 而返回 401。
     * 因此凡是超过两层的公开接口，都要显式写出或用 {@code /**} 覆盖。
     */
    private static final String[] PUBLIC_PATHS = {
            "/api/public/**",
            // 认证
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            // 非遗项目（列表、热门、统计、详情）
            "/api/project/list",
            "/api/project/hot",
            "/api/project/stats/**",
            "/api/project/*",
            // 非遗类别
            "/api/category/**",
            // 传承人（列表、详情）
            "/api/inheritor/list",
            "/api/inheritor/*",
            // 文化资讯（列表、分类、详情、相关推荐）
            "/api/article/list",
            "/api/article/categories",
            "/api/article/detail/**",
            "/api/article/related",
            "/api/article/*",
            // 活动（列表、详情）
            "/api/activity/list",
            "/api/activity/*",
            // 互动（评论列表公开，收藏/点赞/发评论需登录）
            "/api/interaction/comments",
            "/api/interaction/comments/**",
            // AI 问答（游客可用，限流在接口层控制）
            "/api/ai/chat",
            "/api/ai/chat/*/refs",
            // 前台统计概览
            "/api/stats/overview",
            // 首页推荐位
            "/api/banner/list",
            // 本地上传文件的静态资源
            "/api/media/file/**",
            "/uploads/**",
            // 其他
            "/error",
            "/favicon.ico"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final SecurityHandlers.JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final SecurityHandlers.JsonAccessDeniedHandler accessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          SecurityHandlers.JsonAuthenticationEntryPoint authenticationEntryPoint,
                          SecurityHandlers.JsonAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 预检请求放行
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // 公开接口
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // 后台管理接口仅管理员
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其余需要登录
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 密码加密器：BCrypt 加盐哈希，不可逆 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 跨域配置，允许的前端地址见 application.yml 的 ich.cors.allowed-origins */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

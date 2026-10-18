package com.yuxi.ich.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置：本地上传文件的静态资源映射。
 *
 * <p>上传目录（默认 backend/uploads）通过
 * {@code /api/media/file/**} 与 {@code /uploads/**} 对外提供访问；
 * 接入对象存储后可移除本映射。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${ich.media.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 用 toUri() 生成合法的 file: URL（Windows 下路径含盘符与中文，
        // 直接拼字符串会产生 file:D:\... 这种非法 URI 导致资源定位 500）
        String location = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/api/media/file/**")
                .addResourceLocations(location);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location);
    }
}

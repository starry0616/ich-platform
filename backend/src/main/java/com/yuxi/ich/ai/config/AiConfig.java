package com.yuxi.ich.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用 AI 配置类。
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {
}

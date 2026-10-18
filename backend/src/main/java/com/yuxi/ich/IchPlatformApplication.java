package com.yuxi.ich;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 非遗文化传承与互动平台 —— 后端服务启动类。
 *
 * <p>启动后默认监听 8080 端口，接口前缀统一为 /api。
 * 开发环境使用 MySQL（见 application.yml 的 dev profile），
 * 单元/集成测试使用 H2 内存库（test profile）。</p>
 */
@SpringBootApplication
@EnableTransactionManagement
public class IchPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(IchPlatformApplication.class, args);
        System.out.println("""
                ============================================================
                  非遗文化传承与互动平台 后端服务已启动
                  接口地址： http://localhost:8080/api
                  健康检查： http://localhost:8080/api/public/health
                ============================================================
                """);
    }
}

package com.yuxi.ich.common.controller;

import com.yuxi.ich.common.response.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公开探活接口。
 *
 * <p>用于部署后确认服务是否正常，也可作为前端首次联调的靶子接口：
 * <pre>
 * curl http://localhost:8080/api/public/health
 * </pre>
 */
@RestController
@RequestMapping("/api/public")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "ich-platform");
        data.put("status", "UP");
        data.put("time", LocalDateTime.now());
        return Result.success(data);
    }
}

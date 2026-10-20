package com.yuxi.ich.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI 服务连接测试请求（管理员，接口：POST /api/admin/ai/connection/test）。
 *
 * <p>安全约定：
 * <ul>
 *   <li>只传服务商枚举 {@code provider}，<b>不接收自定义 baseUrl</b>，目标地址由后端白名单映射；</li>
 *   <li>{@code apiKey} 仅作为本次请求的临时参数，不落库、不写文件、不回显、不进入运行配置；</li>
 *   <li>{@code model} 可选，缺省时使用该服务商的默认模型。</li>
 * </ul>
 */
public class ConnectionTestRequest {

    /** 服务商枚举：glm（智谱）/ siliconflow（硅基流动） */
    @NotBlank(message = "请选择服务商")
    @Size(max = 32, message = "服务商标识过长")
    private String provider;

    /** 本次测试使用的 API Key，仅临时使用，不保存 */
    @NotBlank(message = "请输入 API Key")
    @Size(max = 200, message = "API Key 长度异常")
    private String apiKey;

    /** 模型名称（可选，缺省使用服务商默认模型） */
    @Size(max = 100, message = "模型名称过长")
    private String model;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

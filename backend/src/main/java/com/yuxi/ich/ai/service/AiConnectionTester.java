package com.yuxi.ich.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuxi.ich.ai.dto.ConnectionTestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AI 服务连接测试器（管理员专用，仅本次请求临时验证，不保存任何配置）。
 *
 * <p>安全设计：
 * <ul>
 *   <li>目标地址来自<b>后端服务商白名单枚举</b>，不接收前端传入的任意 baseUrl，杜绝 SSRF；</li>
 *   <li>API Key 仅作为方法局部变量使用，不写入 {@code AiProperties}、不入库、不写文件、不回显；</li>
 *   <li>不跟随 HTTP 重定向，避免白名单被 30x 绕过；</li>
 *   <li>日志只记录分类/状态码/耗时，绝不打印 Key、Authorization 头或供应商原始响应全文；</li>
 *   <li>探测使用最小非流式请求，限制输出 token 与超时。</li>
 * </ul>
 */
@Component
public class AiConnectionTester {

    private static final Logger log = LoggerFactory.getLogger(AiConnectionTester.class);

    // ---- 结果分类 stage ----
    public static final String SUCCESS = "SUCCESS";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String UPSTREAM_ERROR = "UPSTREAM_ERROR";
    public static final String NETWORK_ERROR = "NETWORK_ERROR";
    public static final String BAD_RESPONSE = "BAD_RESPONSE";
    public static final String INVALID_PROVIDER = "INVALID_PROVIDER";

    /** 服务商白名单：code → 固定地址 / 默认模型 / 展示名 */
    public enum Provider {
        GLM("glm", "https://open.bigmodel.cn/api/paas/v4", "glm-4-flash", "智谱 GLM"),
        SILICONFLOW("siliconflow", "https://api.siliconflow.cn/v1",
                "Qwen/Qwen2.5-7B-Instruct", "硅基流动");

        private final String code;
        private final String baseUrl;
        private final String defaultModel;
        private final String label;

        Provider(String code, String baseUrl, String defaultModel, String label) {
            this.code = code;
            this.baseUrl = baseUrl;
            this.defaultModel = defaultModel;
            this.label = label;
        }

        public String code() {
            return code;
        }

        public String baseUrl() {
            return baseUrl;
        }

        public String defaultModel() {
            return defaultModel;
        }

        public String label() {
            return label;
        }
    }

    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "open.bigmodel.cn", "api.siliconflow.cn");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * 执行一次最小非流式连接探测。
     *
     * @param providerCode 服务商枚举 code
     * @param apiKey       本次临时使用的密钥（不保存）
     * @param model        模型名（可空，用默认模型）
     */
    public ConnectionTestResult test(String providerCode, String apiKey, String model) {
        long start = System.currentTimeMillis();
        Provider provider = resolveProvider(providerCode);
        if (provider == null) {
            return ConnectionTestResult.of(false, INVALID_PROVIDER, null,
                    "不支持的服务商，请从下拉列表选择", 0L, null);
        }
        String baseUrl = provider.baseUrl();
        String usedModel = (model == null || model.isBlank()) ? provider.defaultModel() : model.trim();

        // 防御性白名单校验（地址本就来自枚举，此处二次确认协议/主机/端口）
        String validateError = validateBaseUrl(baseUrl);
        if (validateError != null) {
            return ConnectionTestResult.of(false, INVALID_PROVIDER, null, validateError, 0L, usedModel);
        }

        String endpoint = stripTrailingSlash(baseUrl) + "/chat/completions";
        Map<String, Object> body = Map.of(
                "model", usedModel,
                "stream", false,
                "max_tokens", 16,
                "messages", List.of(Map.of("role", "user", "content", "ping")));

        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    // 密钥仅用于本次请求头，方法返回后局部引用即释放
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long elapsed = System.currentTimeMillis() - start;
            ConnectionTestResult result = classify(response.statusCode(), response.body(), elapsed, usedModel);
            // 日志不含 Key、不含原始响应全文
            log.info("AI 连接测试 provider={} stage={} http={} elapsed={}ms",
                    provider.code(), result.getStage(), result.getHttpStatus(), elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            // 只记录异常类别，不打印消息中可能携带的目标地址/凭证细节
            log.info("AI 连接测试网络异常 provider={} type={} elapsed={}ms",
                    provider.code(), e.getClass().getSimpleName(), elapsed);
            return ConnectionTestResult.of(false, NETWORK_ERROR, null,
                    "无法连接到服务商（网络、DNS、TLS 或超时），请检查网络后重试",
                    elapsed, usedModel);
        }
    }

    // ==================== 纯逻辑（包级可见，便于单元测试） ====================

    /** 按 code 解析白名单服务商，未知返回 null */
    public static Provider resolveProvider(String code) {
        if (code == null) {
            return null;
        }
        for (Provider p : Provider.values()) {
            if (p.code.equalsIgnoreCase(code.trim())) {
                return p;
            }
        }
        return null;
    }

    /**
     * 校验目标地址：仅允许 https、443/默认端口、白名单精确主机名；
     * 拒绝 IP、localhost、内网地址、非标准端口、其他主机。合法返回 null，否则返回错误说明。
     */
    public static String validateBaseUrl(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme();
            if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
                return "仅支持 HTTPS 地址";
            }
            int port = uri.getPort();
            if (port != -1 && port != 443) {
                return "仅允许标准 443 端口";
            }
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "地址缺少主机名";
            }
            String lower = host.toLowerCase();
            if (!ALLOWED_HOSTS.contains(lower)) {
                return "目标主机不在允许列表内";
            }
            if (isIpLiteral(lower) || lower.equals("localhost") || isPrivateHost(lower)) {
                return "禁止访问内网或本机地址";
            }
            return null;
        } catch (Exception e) {
            return "地址格式非法";
        }
    }

    /** 依据 HTTP 状态码与响应体分类，不向前端暴露原始内容 */
    public static ConnectionTestResult classify(int statusCode, String body, long elapsedMs, String model) {
        switch (statusCode) {
            case 200:
                if (hasValidContent(body)) {
                    return ConnectionTestResult.of(true, SUCCESS, 200,
                            "连接成功，模型可正常响应", elapsedMs, model);
                }
                return ConnectionTestResult.of(false, BAD_RESPONSE, 200,
                        "响应格式不符合预期（缺少模型内容）", elapsedMs, model);
            case 400:
                return ConnectionTestResult.of(false, BAD_REQUEST, 400,
                        "请求参数错误，请检查模型名称与请求格式", elapsedMs, model);
            case 401:
                return ConnectionTestResult.of(false, AUTH_FAILED, 401,
                        "认证失败：API Key 无效或已失效", elapsedMs, model);
            case 403:
                return ConnectionTestResult.of(false, FORBIDDEN, 403,
                        "访问被拒绝：账号无权使用该模型或服务", elapsedMs, model);
            case 404:
                return ConnectionTestResult.of(false, NOT_FOUND, 404,
                        "未找到对应接口或模型：请核对模型名称、接口路径或账号权限", elapsedMs, model);
            case 429:
                return ConnectionTestResult.of(false, RATE_LIMITED, 429,
                        "触发频率限制或额度不足，请稍后再试或检查账户额度", elapsedMs, model);
            default:
                if (statusCode >= 500) {
                    return ConnectionTestResult.of(false, UPSTREAM_ERROR, statusCode,
                            "服务商暂时不可用（服务端错误），请稍后重试", elapsedMs, model);
                }
                return ConnectionTestResult.of(false, UPSTREAM_ERROR, statusCode,
                        "连接未成功（HTTP " + statusCode + "）", elapsedMs, model);
        }
    }

    /** 判断 200 响应是否包含非空 choices[0].message.content */
    public static boolean hasValidContent(String body) {
        try {
            JsonNode root = new ObjectMapper().readTree(body == null ? "" : body);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.asText(null);
            return text != null && !text.isBlank();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isIpLiteral(String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            return addr.getHostAddress().equals(host) || host.matches("\\d+(\\.\\d+){3}");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPrivateHost(String host) {
        return host.endsWith(".local") || host.endsWith(".internal")
                || host.startsWith("10.") || host.startsWith("192.168.")
                || host.matches("172\\.(1[6-9]|2\\d|3[01])\\..*");
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}

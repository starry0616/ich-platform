package com.yuxi.ich.ai.dto;

/**
 * AI 服务连接测试结果。
 *
 * <p>只返回必要的诊断信息，<b>绝不包含完整 API Key、Authorization 头或供应商原始响应全文</b>。
 *
 * @param ok 是否连通成功
 * @param stage 结果分类（见 AiConnectionTester 的 stage 常量）
 * @param httpStatus 供应商返回的 HTTP 状态码（网络层失败时为 null）
 * @param message 安全、简洁的中文说明
 * @param elapsedMs 本次探测耗时（毫秒）
 * @param model 实际使用的模型名（回显，不含密钥）
 */
public class ConnectionTestResult {

    private boolean ok;
    private String stage;
    private Integer httpStatus;
    private String message;
    private long elapsedMs;
    private String model;

    public ConnectionTestResult() {
    }

    public static ConnectionTestResult of(boolean ok, String stage, Integer httpStatus,
                                          String message, long elapsedMs, String model) {
        ConnectionTestResult r = new ConnectionTestResult();
        r.ok = ok;
        r.stage = stage;
        r.httpStatus = httpStatus;
        r.message = message;
        r.elapsedMs = elapsedMs;
        r.model = model;
        return r;
    }

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public void setElapsedMs(long elapsedMs) {
        this.elapsedMs = elapsedMs;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }
}

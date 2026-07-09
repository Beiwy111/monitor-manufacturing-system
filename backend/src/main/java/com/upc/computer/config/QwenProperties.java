package com.upc.computer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 通义千问（DashScope 兼容模式）配置。
 */
@Component
@ConfigurationProperties(prefix = "qwen")
public class QwenProperties {

    /** 是否启用 AI 生成报告 */
    private boolean enabled = true;

    /** DashScope API Key（sk- 开头） */
    private String apiKey = "";

    /** OpenAI 兼容接口地址 */
    private String apiUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    /** 模型名称，如 qwen-plus、qwen-turbo */
    private String model = "qwen-plus";

    private double temperature = 0.3;

    private int maxTokens = 1024;

    private int timeoutMs = 30000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}

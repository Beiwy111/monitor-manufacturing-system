package com.upc.computer.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MES Agent 的 Spring AI 配置。
 *
 * <p>该配置与现有 {@code com.upc.computer.config.AiProperties} 隔离，避免影响
 * OCR、语音识别等存量 AI 能力。</p>
 */
@ConfigurationProperties(prefix = "mes.ai")
public class AiProperties {

    private boolean enabled;
    private String provider = "deepseek";
    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private double temperature = 0.2;
    private int maxTokens = 4096;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
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
}

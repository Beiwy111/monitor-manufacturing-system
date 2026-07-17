package com.upc.computer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 财务及管理员全局 AI 分析专用 DeepSeek 配置，与现有 deepseek.* Key 完全隔离。 */
@Component
@ConfigurationProperties(prefix = "deepseek-finance")
public class FinanceDeepseekProperties {

    private String apiKey = "";
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";
    private String model = "deepseek-v4-flash";
    private double temperature = 0.2;
    private int maxTokens = 4096;

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
}

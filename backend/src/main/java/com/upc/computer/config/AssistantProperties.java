package com.upc.computer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 售后语音助手配置。前缀 assistant。
 * asr：语音识别。走阿里云百炼 OpenAI 兼容 /chat/completions + 语音识别模型（qwen3-asr-flash），
 *      复用 ai.* 的 api-key 与 base-url（也可在此单独覆写）。
 * nlu：全局自然语言问答与写操作理解（默认复用原有 deepseek.*；解析失败自动回退规则解析）。
 */
@Component
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    /** 置信度阈值，低于此值让用户复述/澄清 */
    private double confidenceThreshold = 0.55;

    private Asr asr = new Asr();
    private Nlu nlu = new Nlu();

    public double getConfidenceThreshold() { return confidenceThreshold; }
    public void setConfidenceThreshold(double confidenceThreshold) { this.confidenceThreshold = confidenceThreshold; }

    public Asr getAsr() { return asr; }
    public void setAsr(Asr asr) { this.asr = asr; }

    public Nlu getNlu() { return nlu; }
    public void setNlu(Nlu nlu) { this.nlu = nlu; }

    /** 语音识别（百炼 qwen3-asr-flash，OpenAI 兼容接口） */
    public static class Asr {
        /** true：不调真实接口，返回样例文本，便于无网络联调 */
        private boolean mock = false;
        /** 语音识别模型 */
        private String model = "qwen3-asr-flash";
        /** 可覆写 api-key（留空则用 ai.api-key） */
        private String apiKey;
        /** 可覆写 base-url（留空则用 ai.base-url，即 .../compatible-mode/v1） */
        private String baseUrl;

        public boolean isMock() { return mock; }
        public void setMock(boolean mock) { this.mock = mock; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }

    /** 全局问答与自然语言理解（默认复用原有 deepseek.*，也可单独覆写） */
    public static class Nlu {
        /** true：强制走规则解析，不调用大模型（离线联调用） */
        private boolean mock = false;
        /** 文本模型名称。 */
        private String model = "deepseek-chat";
        /** 可覆写 api-key（留空则用原有 deepseek.api-key，最后兼容 ai.api-key） */
        private String apiKey;
        /** 可覆写完整 chat/completions 地址或兼容 API 根地址（留空则用 deepseek.api-url） */
        private String baseUrl;
        /** 模型单次回答最大输出 token；详细分析需要高于普通意图识别的默认值。 */
        private int maxTokens = 8192;
        /** 模型回答超时秒数；<= 0 表示不限制生成耗时，只保留连接建立超时。 */
        private int timeoutSeconds = 0;

        public boolean isMock() { return mock; }
        public void setMock(boolean mock) { this.mock = mock; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public int getMaxTokens() { return maxTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }
}

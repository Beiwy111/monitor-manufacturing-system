package com.upc.computer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 售后语音助手配置。前缀 assistant。
 * asr：语音识别。走阿里云百炼 OpenAI 兼容 /chat/completions + 语音识别模型（qwen3-asr-flash），
 *      复用 ai.* 的 api-key 与 base-url（也可在此单独覆写）。
 * nlu：自然语言理解（复用 deepseek.* 文本模型；解析失败自动回退规则解析）。
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

    /** 自然语言理解（百炼 qwen-plus，复用 ai.api-key/ai.base-url） */
    public static class Nlu {
        /** true：强制走规则解析，不调用大模型（离线联调用） */
        private boolean mock = false;
        /** NLU 文本模型（百炼上非思维链、出 JSON 干净的模型） */
        private String model = "qwen-plus";
        /** 可覆写 api-key（留空则用 ai.api-key） */
        private String apiKey;
        /** 可覆写 base-url（留空则用 ai.base-url） */
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
}

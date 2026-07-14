package com.upc.computer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "yolo")
public class YoloProperties {

    private boolean enabled = true;
    private String baseUrl = "http://localhost:8000";
    private String predictPath = "/predict/json";
    private int timeoutSeconds = 90;
    private boolean mock = false;
    /** 本地 YOLO 开源项目目录（Mobile-Phone-Defect） */
    private String projectDir = "";

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getPredictPath() {
        return predictPath;
    }

    public void setPredictPath(String predictPath) {
        this.predictPath = predictPath;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }
}

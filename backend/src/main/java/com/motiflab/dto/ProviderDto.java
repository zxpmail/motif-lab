package com.motiflab.dto;

/**
 * Provider 设置的读写 DTO。
 * GET 时 apiKey 为脱敏值；PUT 时 apiKey 为空表示保留原密钥。
 * 关联：ProviderController、ProviderService。
 */
public class ProviderDto {

    private String apiKey;
    private String baseUrl;
    private String model;
    private boolean enabled;

    public ProviderDto() {}

    public ProviderDto(String apiKey, String baseUrl, String model, boolean enabled) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
        this.enabled = enabled;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

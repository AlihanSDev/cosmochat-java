package cosmochat.huggingface.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;

@ConfigurationProperties(prefix = "huggingface")
@Validated
public class HuggingFaceProperties {

    @NotEmpty(message = "HF_TOKEN is required")
    private String token;

    private String apiBaseUrl = "https://router.huggingface.co/v1";
    private String modelId = "Qwen/Qwen2.5-Coder-7B-Instruct";
    private String nscaleSuffix = ":nscale";

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getNscaleSuffix() {
        return nscaleSuffix;
    }

    public void setNscaleSuffix(String nscaleSuffix) {
        this.nscaleSuffix = nscaleSuffix;
    }

    public String getFullModelId() {
        return modelId + nscaleSuffix;
    }

    public String getChatEndpoint() {
        return apiBaseUrl + "/chat/completions";
    }
}

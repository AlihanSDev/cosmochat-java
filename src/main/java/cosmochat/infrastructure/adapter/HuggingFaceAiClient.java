package cosmochat.infrastructure.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import cosmochat.domain.port.AiPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HuggingFaceAiClient — directly calls HuggingFace Inference API.
 * Uses the API token from environment or system properties.
 */
public class HuggingFaceAiClient implements AiPort {
    private static final Logger logger = Logger.getLogger(HuggingFaceAiClient.class.getName());
    // Use HuggingFace Inference API directly
    private static final String API_URL = "https://router.huggingface.co/v1/chat/completions";
    private static final String MODEL_ENV = "HF_MODEL_ID";
    private static final String TOKEN_ENV = "HF_TOKEN";

    private final HttpClient httpClient;
    private final Gson gson;
    private final String token;
    private final String modelId;

    public HuggingFaceAiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
        // Get token from environment/properties
        this.token = System.getenv(TOKEN_ENV) != null ? System.getenv(TOKEN_ENV)
                    : System.getProperty(TOKEN_ENV);
        this.modelId = System.getenv(MODEL_ENV) != null ? System.getenv(MODEL_ENV)
                     : "Qwen/Qwen2.5-Coder-7B-Instruct:featherless-ai";

        if (token == null || token.isBlank()) {
            logger.warning(TOKEN_ENV + " environment variable not set. HuggingFace API will fail.");
        }
        logger.info("HuggingFaceAiClient initialized: model=" + modelId + ", tokenPresent=" + (token != null && !token.isBlank()));
    }

    @Override
    public boolean isAvailable() {
        if (token == null || token.isBlank()) {
            return false;
        }
        // Assume available; actual check on first call
        return true;
    }

    @Override
    public String sendMessage(String message) throws Exception {
        if (token == null || token.isBlank()) {
            throw new Exception("HuggingFace API token (" + TOKEN_ENV + ") is not configured. Set environment variable.");
        }

        // Build OpenAI-compatible chat completion request
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", modelId);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", message);

        JsonArray messages = new JsonArray();
        messages.add(userMsg);
        requestBody.add("messages", messages);

        requestBody.addProperty("max_tokens", 512);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("top_p", 0.9);

        String jsonBody = gson.toJson(requestBody);
        logger.fine("Sending request to HuggingFace: model=" + modelId + ", message=" + message.substring(0, Math.min(50, message.length())));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.fine("HuggingFace response status: " + response.statusCode());
        logger.fine("HuggingFace response body: " + response.body());

        if (response.statusCode() == 200) {
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject msg = firstChoice.getAsJsonObject("message");
                if (msg != null && msg.has("content")) {
                    return msg.get("content").getAsString();
                }
            }
            throw new Exception("Unexpected response format from HuggingFace: " + response.body());
        } else {
            // Parse error from HF
            String errorMsg = "HuggingFace API error (HTTP " + response.statusCode() + ")";
            try {
                JsonObject errJson = gson.fromJson(response.body(), JsonObject.class);
                if (errJson.has("error")) {
                    errorMsg += ": " + errJson.get("error").getAsString();
                } else {
                    errorMsg += ": " + response.body();
                }
            } catch (Exception ex) {
                errorMsg += ": " + response.body();
            }
            throw new Exception(errorMsg);
        }
    }
}

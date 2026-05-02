package cosmochat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Service for communicating with the Python AI API.
 * Handles sending messages to Qwen2.5 model and receiving responses.
 */
public class AiService {
    private static final String API_URL = "http://127.0.0.1:5001";
    private static final String CHAT_ENDPOINT = "/chat";
    private static final String HEALTH_ENDPOINT = "/health";
    
    private final HttpClient httpClient;
    private final Gson gson;
    private String modelStatus = "unknown";
    
    public AiService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
    }
    
    /**
     * Checks if the Python API server is running and model is loaded.
     */
    public boolean isApiAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + HEALTH_ENDPOINT))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
                
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                modelStatus = json.get("loaded").getAsBoolean() ? "loaded" : "not_loaded";
                return json.get("loaded").getAsBoolean();
            }
            return false;
        } catch (Exception e) {
            modelStatus = "unavailable";
            return false;
        }
    }
    
    /**
     * Sends a message to the AI and gets a response.
     * This method is blocking and should be called from a background thread.
     */
    public String sendMessage(String message) throws Exception {
        return sendMessage(message, 512, 0.7);
    }
    
    /**
     * Sends a message to the AI with custom parameters.
     */
    public String sendMessage(String message, int maxTokens, double temperature) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        payload.addProperty("max_tokens", maxTokens);
        payload.addProperty("temperature", temperature);
        
        String requestBody = gson.toJson(payload);
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL + CHAT_ENDPOINT))
            .timeout(Duration.ofSeconds(60))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();
            
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() == 200) {
            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
            return json.get("response").getAsString();
        } else if (response.statusCode() == 503) {
            throw new Exception("AI model is not loaded. Please ensure the Python API server is running and the model is loaded.");
        } else {
            throw new Exception("API error: " + response.statusCode() + " - " + response.body());
        }
    }
    
    /**
     * Gets the current model status.
     */
    public String getModelStatus() {
        return modelStatus;
    }
}

package cosmochat.infrastructure.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import cosmochat.domain.port.AiPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PythonAiClient implements AiPort {
    private static final Logger logger = Logger.getLogger(PythonAiClient.class.getName());
    private static final String API_URL = "http://127.0.0.1:5001";
    private static final String CHAT_ENDPOINT = "/chat";
    private static final String HEALTH_ENDPOINT = "/health";

    private final HttpClient httpClient;
    private final Gson gson;
    private String modelStatus = "unknown";
    private static final int MAX_RETRIES = 2;
    private static final int RETRY_DELAY_MS = 1000;

    public PythonAiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    @Override
    public boolean isAvailable() {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.fine("Checking availability (attempt %d) against %s".formatted(attempt, API_URL + HEALTH_ENDPOINT));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + HEALTH_ENDPOINT))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.fine("Health response status=%d body=%s".formatted(response.statusCode(), response.body()));

                if (response.statusCode() == 200) {
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    boolean loaded = json.has("loaded") && json.get("loaded").getAsBoolean();
                    modelStatus = loaded ? "loaded" : "not_loaded";
                    return loaded;
                }

                logger.warning("Health check failed with HTTP %d".formatted(response.statusCode()));
                return false;
            } catch (Exception e) {
                if (attempt < MAX_RETRIES) {
                    logger.fine("Health check attempt %d failed: %s, retrying...".formatted(attempt, e.getMessage()));
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    modelStatus = "unavailable";
                    logger.log(Level.WARNING, "AI availability check failed after " + MAX_RETRIES + " attempts", e);
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public String sendMessage(String message) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("message", message);
        payload.addProperty("max_tokens", 512);
        payload.addProperty("temperature", 0.7);

        String requestBody = gson.toJson(payload);
        logger.info("Sending AI request: %s".formatted(message.length() > 100 ? message.substring(0, 100) + "..." : message));
        logger.fine("Request body: %s".formatted(requestBody));

        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                logger.fine("Attempt %d to send AI request".formatted(attempt));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL + CHAT_ENDPOINT))
                        .timeout(Duration.ofSeconds(60))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                logger.info("AI request completed: status=%d".formatted(response.statusCode()));
                logger.fine("AI response body: %s".formatted(response.body()));

                if (response.statusCode() == 200) {
                    JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                    if (json.has("response") && !json.get("response").isJsonNull()) {
                        return json.get("response").getAsString();
                    }
                    if (json.has("error")) {
                        String errorText = json.get("error").getAsString();
                        logger.warning("AI API returned error field: %s".formatted(errorText));
                        throw new Exception("AI API error: " + errorText);
                    }
                    logger.warning("AI API returned unexpected JSON structure");
                    throw new Exception("AI API returned unexpected response: " + response.body());
                } else if (response.statusCode() == 503) {
                    logger.warning("AI model not loaded (503)");
                    throw new Exception("AI model is not loaded. Please ensure the Python API server is running.");
                } else {
                    String body = response.body();
                    logger.warning("AI API call failed: status=%d body=%s".formatted(response.statusCode(), body));
                    throw new Exception("API error: " + response.statusCode() + " - " + body);
                }
            } catch (Exception e) {
                lastException = e;
                logger.warning("Attempt %d failed: %s".formatted(attempt, e.getMessage()));

                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }

        throw new Exception("Failed after " + MAX_RETRIES + " attempts. Last error: " + lastException.getMessage());
    }
}

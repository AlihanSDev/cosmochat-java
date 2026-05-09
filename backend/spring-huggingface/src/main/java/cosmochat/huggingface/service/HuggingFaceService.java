package cosmochat.huggingface.service;

import cosmochat.huggingface.config.HuggingFaceProperties;
import cosmochat.huggingface.dto.ChatCompletionRequest;
import cosmochat.huggingface.dto.ChatCompletionResponse;
import cosmochat.huggingface.exception.HuggingFaceApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class HuggingFaceService {
    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceService.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 1000;

    private final HuggingFaceProperties properties;
    private final RestTemplate restTemplate;

    public HuggingFaceService(HuggingFaceProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public String generateResponse(String userMessage) {
        ChatCompletionRequest request = new ChatCompletionRequest(
            properties.getFullModelId(),
            List.of(new ChatCompletionRequest.Message("user", userMessage)),
            512,
            0.7
        );

        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(properties.getToken());
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

                logger.info("Sending request to HuggingFace (attempt {}/{})", attempt, MAX_RETRIES);
                ResponseEntity<ChatCompletionResponse> response = restTemplate.exchange(
                    properties.getChatEndpoint(),
                    HttpMethod.POST,
                    entity,
                    ChatCompletionResponse.class
                );

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    ChatCompletionResponse body = response.getBody();
                    if (body.choices() != null && !body.choices().isEmpty()) {
                        String aiMessage = body.choices().get(0).message().content();
                        logger.info("Received response from HuggingFace ({} tokens)", 
                            body.usage() != null ? body.usage().totalTokens() : "unknown");
                        return aiMessage;
                    }
                }

                throw new HuggingFaceApiException("Empty response from HuggingFace", 
                    HuggingFaceApiException.ErrorType.UNKNOWN_ERROR);

            } catch (HttpClientErrorException e) {
                lastException = e;
                int statusCode = e.getStatusCode().value();
                logger.warn("HuggingFace API error: HTTP {} - {}", statusCode, e.getResponseBodyAsString());

                HuggingFaceApiException.ErrorType errorType = classifyErrorByStatus(statusCode, e.getResponseBodyAsString());
                boolean retryable = isRetryableError(errorType, statusCode);

                if (!retryable || attempt == MAX_RETRIES) {
                    throw new HuggingFaceApiException(
                        getMessageForErrorType(errorType, statusCode, e.getResponseBodyAsString()),
                        errorType,
                        statusCode
                    );
                }

            } catch (RestClientException e) {
                lastException = e;
                logger.warn("Network error (attempt {}/{}): {}", attempt, MAX_RETRIES, e.getMessage());

                boolean isNetworkError = e.getMessage().toLowerCase().contains("timeout") ||
                                        e.getMessage().toLowerCase().contains("connection refused") ||
                                        e.getMessage().toLowerCase().contains("unable to connect");

                if (!isNetworkError || attempt == MAX_RETRIES) {
                    throw new HuggingFaceApiException(
                        "Ошибка подключения к HuggingFace. Проверьте интернет-соединение.",
                        HuggingFaceApiException.ErrorType.NETWORK_ERROR
                    );
                }
            } catch (Exception e) {
                lastException = e;
                logger.error("Unexpected error", e);
                if (attempt == MAX_RETRIES) {
                    throw new HuggingFaceApiException(
                        "Неизвестная ошибка: " + e.getMessage(),
                        HuggingFaceApiException.ErrorType.UNKNOWN_ERROR
                    );
                }
            }

            // Exponential backoff before retry
            if (attempt < MAX_RETRIES) {
                long delay = INITIAL_DELAY_MS * (1L << (attempt - 1));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new HuggingFaceApiException("Request interrupted", HuggingFaceApiException.ErrorType.UNKNOWN_ERROR);
                }
            }
        }

        throw new HuggingFaceApiException(
            "Все попытки исчерпаны. Последняя ошибка: " + 
                (lastException != null ? lastException.getMessage() : "unknown"),
            HuggingFaceApiException.ErrorType.UNKNOWN_ERROR
        );
    }

    private HuggingFaceApiException.ErrorType classifyErrorByStatus(int statusCode, String body) {
        return switch (statusCode) {
            case 401, 403 -> HuggingFaceApiException.ErrorType.AUTHENTICATION_ERROR;
            case 429 -> HuggingFaceApiException.ErrorType.RATE_LIMIT_ERROR;
            case 503 -> HuggingFaceApiException.ErrorType.MODEL_NOT_LOADED;
            case 400 -> HuggingFaceApiException.ErrorType.BAD_REQUEST;
            case 500, 502, 504 -> HuggingFaceApiException.ErrorType.SERVER_ERROR;
            default -> HuggingFaceApiException.ErrorType.UNKNOWN_ERROR;
        };
    }

    private boolean isRetryableError(HuggingFaceApiException.ErrorType errorType, int statusCode) {
        return switch (errorType) {
            case NETWORK_ERROR -> true;
            case RATE_LIMIT_ERROR -> false; // 429 — не ретраить
            case MODEL_NOT_LOADED -> false; // 503 — модель грузится, можно подождать, но не ретраить часто
            case SERVER_ERROR -> true; // 5xx — можно ретраить
            case AUTHENTICATION_ERROR, BAD_REQUEST -> false; // клиентские ошибки
            case UNKNOWN_ERROR -> true;
        };
    }

    private String getMessageForErrorType(HuggingFaceApiException.ErrorType errorType, int statusCode, String body) {
        return switch (errorType) {
            case AUTHENTICATION_ERROR -> "Ошибка авторизации HuggingFace (HTTP " + statusCode + "). Проверьте ваш API токен.";
            case RATE_LIMIT_ERROR -> "Превышен лимит запросов HuggingFace (HTTP 429). Подождите несколько минут.";
            case MODEL_NOT_LOADED -> "Модель '" + properties.getFullModelId() + "' на HuggingFace ещё не загружена (HTTP 503). Подождите 1-2 минуты и попробуйте снова.";
            case SERVER_ERROR -> "Ошибка сервера HuggingFace (HTTP " + statusCode + "). Попробуйте позже.";
            case BAD_REQUEST -> "Неверный запрос к HuggingFace API (HTTP 400). Проверьте параметры запроса.";
            case NETWORK_ERROR -> "Не удалось подключиться к HuggingFace. Проверьте интернет-соединение.";
            case UNKNOWN_ERROR -> "Неизвестная ошибка от HuggingFace (HTTP " + statusCode + "): " + body;
        };
    }
}

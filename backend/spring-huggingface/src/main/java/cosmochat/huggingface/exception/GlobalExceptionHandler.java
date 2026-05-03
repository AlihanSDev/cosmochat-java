package cosmochat.huggingface.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HuggingFaceApiException.class)
    public ResponseEntity<Map<String, Object>> handleHuggingFaceApiException(HuggingFaceApiException ex) {
        String userMessage = switch (ex.getErrorType()) {
            case AUTHENTICATION_ERROR -> "Ошибка авторизации HuggingFace. Проверьте ваш API токен (HF_TOKEN).";
            case RATE_LIMIT_ERROR -> "Превышен лимит запросов HuggingFace API. Подождите несколько минут и попробуйте снова.";
            case MODEL_NOT_LOADED -> "Модель на HuggingFace ещё не загружена. Подождите 1-2 минуты и попробуйте снова.";
            case SERVER_ERROR -> "Внутренняя ошибка сервера HuggingFace. Попробуйте позже.";
            case BAD_REQUEST -> "Неверный запрос к модели. Пожалуйста, переформулируйте ваш запрос.";
            case NETWORK_ERROR -> "Ошибка подключения к HuggingFace. Проверьте интернет-соединение.";
            case UNKNOWN_ERROR -> "Неизвестная ошибка: " + ex.getMessage();
        };

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", userMessage,
                "error_type", ex.getErrorType().name(),
                "timestamp", Instant.now().toString()
            ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of(
                "error", "Неверный запрос: " + ex.getMessage(),
                "error_type", "BAD_REQUEST",
                "timestamp", Instant.now().toString()
            ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of(
                "error", "Внутренняя ошибка сервера: " + ex.getMessage(),
                "error_type", "UNKNOWN_ERROR",
                "timestamp", Instant.now().toString()
            ));
    }
}

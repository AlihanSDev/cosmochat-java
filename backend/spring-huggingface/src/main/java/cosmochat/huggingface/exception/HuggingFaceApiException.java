package cosmochat.huggingface.exception;

public class HuggingFaceApiException extends RuntimeException {
    private final ErrorType errorType;
    private final Integer statusCode;

    public HuggingFaceApiException(String message, ErrorType errorType, Integer statusCode) {
        super(message);
        this.errorType = errorType;
        this.statusCode = statusCode;
    }

    public HuggingFaceApiException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.statusCode = null;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public enum ErrorType {
        AUTHENTICATION_ERROR,      // 401, 403
        RATE_LIMIT_ERROR,          // 429
        MODEL_NOT_LOADED,          // 503 — модель не загружена на HF
        SERVER_ERROR,              // 500, 502, 504
        BAD_REQUEST,               // 400
        NETWORK_ERROR,             // таймаут,连接 refused
        UNKNOWN_ERROR
    }
}

# HuggingFace AI Service (Spring Boot)

Spring Boot сервис для работы с HuggingFace Inference API (OpenAI совместимый).

## 🔧 Настройка

1. **Скопируйте `.env.example` в `.env`**:
   ```bash
   cp .env.example .env
   ```

2. **Получите токен HuggingFace**:
   - Зайдите на https://huggingface.co/settings/tokens
   - Создайте новый токен с правом `read`
   - Вставьте его в `.env`:
     ```
     HF_TOKEN=hf_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
     ```

3. **Настройте модель** (опционально):
   ```
   HF_MODEL_ID=Qwen/Qwen2.5-Coder-7B-Instruct
   ```

## 🚀 Запуск

### Через Maven:
```bash
./mvnw spring-boot:run
```

### Или через JAR:
```bash
./mvnw package
java -jar target/huggingface-service-1.0.0.jar
```

Сервер запустится на `http://localhost:8080`.

## 📡 API Endpoints

### POST `/api/v1/chat`
**Request:**
```json
{
  "message": "What is the capital of France?",
  "max_tokens": 512,
  "temperature": 0.7
}
```

**Response (success):**
```json
{
  "response": "The capital of France is Paris.",
  "model": "Qwen2.5-Coder-7B-Instruct (HuggingFace)",
  "provider": "huggingface"
}
```

**Response (error):**
```json
{
  "error": "Ошибка авторизации HuggingFace. Проверьте ваш API токен.",
  "error_type": "AUTHENTICATION_ERROR",
  "timestamp": "2026-05-03T13:42:41+05:00"
}
```

### GET `/api/v1/health`
**Response:**
```json
{
  "status": "ok",
  "service": "huggingface-spring",
  "model": "Qwen2.5-Coder-7B-Instruct"
}
```

## 🔄 Ретеи (Retries)

Сервис автоматически повторяет запросы при:
- Сетевых ошибках (таймауты, connection refused)
- HTTP 5xx (500, 502, 504)
- Не загруженной модели (503) — 1 повтор через 1с

**НЕ повторяет:**
- 429 Rate Limit
- 401/403 Auth errors
- 400 Bad Request

Exponential backoff: 1s → 2s → 4s

## 📊 Типы ошибок

| Тип ошибки | HTTP | Описание | Решение |
|-----------|------|----------|---------|
| `AUTHENTICATION_ERROR` | 401, 403 | Неверный/отсутствующий токен | Проверьте `HF_TOKEN` |
| `RATE_LIMIT_ERROR` | 429 | Превышен лимит запросов | Подождите несколько минут |
| `MODEL_NOT_LOADED` | 503 | Модель ещё не загружена на HF | Подождите 1-2 минуты |
| `SERVER_ERROR` | 500, 502, 504 | Ошибка сервера HF | Попробуйте позже |
| `BAD_REQUEST` | 400 | Неверный запрос | Переформулируйте |
| `NETWORK_ERROR` | — | Ошибка сети | Проверьте интернет |

## 🐳 Docker (опционально)

```dockerfile
FROM eclipse-temurin:17-jdk-alpine
COPY target/huggingface-service-1.0.0.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 📁 Структура проекта

```
backend/spring-huggingface/
├── src/main/java/cosmochat/huggingface/
│   ├── HuggingFaceApplication.java      # Spring Boot main class
│   ├── config/
│   │   ├── HuggingFaceProperties.java   # Конфиг (token, model, url)
│   │   └── RestConfig.java              # RestTemplate bean
│   ├── controller/
│   │   └── ChatController.java          # REST API endpoints
│   ├── service/
│   │   └── HuggingFaceService.java      # Business logic + retries
│   ├── dto/
│   │   ├── ChatCompletionRequest.java
│   │   └── ChatCompletionResponse.java
│   └── exception/
│       ├── HuggingFaceApiException.java
│       └── GlobalExceptionHandler.java
├── .env.example
├── pom.xml
└── README.md
```

## 🔐 Безопасность

- Токен хранится в `.env` (файл `.gitignore` уже добавлен)
- Не коммитьте `.env`!
- Для production используйте переменные окружения или secret manager

## 📝 Логирование

Логи выводятся в stdout (SLF4J Simple). Для продвинутого логирования добавьте `logback-spring.xml`.

## ⚙️ Конфигурация через переменные окружения

```bash
export HF_TOKEN=your_token
export HF_API_BASE_URL=https://router.huggingface.co/v1
export HF_MODEL_ID=Qwen/Qwen2.5-Coder-7B-Instruct
export SERVER_PORT=8080
```

Или через `application.yml`:
```yaml
huggingface:
  token: ${HF_TOKEN}
  api-base-url: https://router.huggingface.co/v1
  model-id: Qwen/Qwen2.5-Coder-7B-Instruct
```

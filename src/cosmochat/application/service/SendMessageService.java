package cosmochat.application.service;

import cosmochat.application.port.SendMessage;
import cosmochat.application.port.UsageTracking;
import cosmochat.application.dto.SendMessageCommand;
import cosmochat.application.dto.SendMessageResult;
import cosmochat.domain.*;
import cosmochat.domain.port.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class SendMessageService implements SendMessage {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 500;
    
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AiPort aiPort;
    private final UsageTracking usageTracking;

    public SendMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository,
            AiPort aiPort,
            UsageTracking usageTracking
    ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.aiPort = aiPort;
        this.usageTracking = usageTracking;
    }

    @Override
    public SendMessageResult execute(SendMessageCommand command) throws Exception {
        UserId userId = command.userId();
        ChatId chatId = command.chatId();
        String text = command.text();
        String model = command.model();

        // Validate user
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Check rate limit
        if (!usageTracking.canSendMessage(userId)) {
            throw new IllegalStateException("Rate limit exceeded (100 messages/hour)");
        }

        // Determine chat: if null, create new
        Chat chat;
        if (chatId == null) {
            String title = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            chat = chatRepository.save(new Chat(
                    new ChatId(0),
                    userId,
                    title,
                    Instant.now().toEpochMilli()
            ));
        } else {
            chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        }

        // Save user message
        Message userMessage = new Message(
                new MessageId(0),
                chat.getId(),
                Role.USER,
                text,
                Instant.now().toEpochMilli()
        );
        messageRepository.save(userMessage);

        // Call AI with retries
        String aiResponse;
        try {
            aiResponse = callAiWithRetry(text, model);
        } catch (Exception e) {
            // Re-throw with user-friendly message already set
            throw e;
        }

        // Save AI message
        Message aiMessage = new Message(
                new MessageId(0),
                chat.getId(),
                Role.AI,
                aiResponse,
                Instant.now().toEpochMilli()
        );
        messageRepository.save(aiMessage);

        // Increment usage
        usageTracking.incrementMessageCount(userId);

        return new SendMessageResult(chat.getId(), userMessage, aiMessage);
    }
    
    private String callAiWithRetry(String text, String model) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return aiPort.sendMessage(text);
            } catch (Exception e) {
                lastException = e;
                if (!isRetryable(e) || attempt == MAX_RETRIES) {
                    String userMessage = getUserFriendlyErrorMessage(e, model);
                    throw new Exception(userMessage, e);
                }
                // Exponential backoff: 0.5s, 1s, 2s
                long delay = INITIAL_DELAY_MS * (1L << (attempt - 1));
                Thread.sleep(delay);
            }
        }
        throw new Exception(getUserFriendlyErrorMessage(lastException, model), lastException);
    }
    
    private boolean isRetryable(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        // Network errors
        if (lower.contains("connection refused") || lower.contains("connectexception") ||
            lower.contains("timed out") || lower.contains("timeout") ||
            lower.contains("unable to connect") || lower.contains("reset by peer") ||
            lower.contains("no route to host")) {
            return true;
        }
        // HTTP status codes that are retryable
        if (lower.contains("503") || lower.contains("502") || lower.contains("500") ||
            lower.contains("429") || lower.contains("408")) {
            return true;
        }
        return false;
    }
    
    private String getUserFriendlyErrorMessage(Exception e, String model) {
        String msg = e.getMessage();
        if (msg == null) msg = e.toString();
        String lower = msg.toLowerCase();
        
        String prefix = "❌ ";
        
        // Network errors
        if (lower.contains("connection refused") || lower.contains("connectexception") ||
            lower.contains("timed out") || lower.contains("timeout") ||
            lower.contains("unable to connect") || lower.contains("reset by peer")) {
            if (model.contains("Qwen 1.5B") || model.contains("CosmoChat Gateway")) {
                return prefix + "Не удалось подключиться к локальному Python API серверу. Убедитесь, что запущен backend/qwen_api.py.";
            } else {
                return prefix + "Ошибка подключения к API модели. Проверьте интернет-соединение и правильность API ключа.";
            }
        }
        
        // Model not loaded (503)
        if (lower.contains("503") || lower.contains("not loaded")) {
            if (model.contains("Qwen 1.5B") || model.contains("CosmoChat Gateway")) {
                return prefix + "Локальная модель Qwen не загружена. Запустите Python API сервер и дождитесь полной загрузки модели.";
            } else if (model.contains("HuggingFace")) {
                return prefix + "Модель на HuggingFace недоступна. Проверьте ваш API токен и доступ к модели.";
            } else {
                return prefix + "Модель не загружена. Проверьте настройки API и попробуйте позже.";
            }
        }
        
        // Rate limit (429)
        if (lower.contains("429") || lower.contains("rate limit")) {
            if (model.contains("HuggingFace")) {
                return prefix + "Превышен лимит запросов HuggingFace API. Пожалуйста, подождите несколько минут.";
            } else if (model.contains("Mistral") || model.contains("Deepseek")) {
                return prefix + "Превышен лимит запросов API. Подождите и попробуйте снова.";
            } else {
                return prefix + "Превышен лимит запросов. Пожалуйста, подождите минуту.";
            }
        }
        
        // Authentication errors (401, 403)
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized") || lower.contains("forbidden")) {
            if (model.contains("HuggingFace")) {
                return prefix + "Ошибка авторизации HuggingFace. Проверьте ваш API токен.";
            } else if (model.contains("Mistral")) {
                return prefix + "Ошибка авторизации Mistral API. Проверьте ваш API ключ.";
            } else if (model.contains("Deepseek")) {
                return prefix + "Ошибка авторизации Deepseek API. Проверьте ваш API ключ.";
            } else {
                return prefix + "Ошибка авторизации. Проверьте настройки API.";
            }
        }
        
        // Bad request (400)
        if (lower.contains("400") || lower.contains("bad request")) {
            return prefix + "Неверный запрос к модели. Пожалуйста, попробуйте переформулировать ваш запрос.";
        }
        
        // Server errors (500, 502)
        if (lower.contains("500") || lower.contains("502")) {
            return prefix + "Внутренняя ошибка сервера модели. Попробуйте позже.";
        }
        
        // Default fallback
        return prefix + "Ошибка при генерации ответа: " + msg;
    }
}

package cosmochat.application.service;

import cosmochat.application.port.SendMessage;
import cosmochat.application.port.UsageTracking;
import cosmochat.application.dto.SendMessageCommand;
import cosmochat.application.dto.SendMessageResult;
import cosmochat.domain.*;
import cosmochat.domain.port.*;
import cosmochat.infrastructure.adapter.AiPortSelector;

import java.time.Instant;

public class SendMessageService implements SendMessage {
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_DELAY_MS = 500;
    
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AiPortSelector aiPortSelector;
    private final UsageTracking usageTracking;

    public SendMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository,
            AiPortSelector aiPortSelector,
            UsageTracking usageTracking
    ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.aiPortSelector = aiPortSelector;
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

        // Call AI with retry logic (passing model-aware AiPort)
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
        AiPort aiPort = aiPortSelector.getPortForModel(model);
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return aiPort.sendMessage(text);
            } catch (Exception e) {
                lastException = e;
                if (!isRetryable(e) || attempt == MAX_RETRIES) {
                    String userMessage = getUserFriendlyErrorMessage(e, model);
                    throw new Exception(userMessage, e);
                }
                // Exponential backoff
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
            } else if (model.contains("HuggingFace")) {
                return prefix + "Не удалось подключиться к HuggingFace сервису. Проверьте интернет-соединение и токен.";
            } else {
                return prefix + "Ошибка подключения к API модели. Проверьте интернет-соединение.";
            }
        }

        // HuggingFace specific errors (show raw message)
        if (lower.contains("huggingface api error")) {
            return prefix + "HuggingFace: " + msg;
        }

        // Model not loaded / service unavailable (503)
        if (lower.contains("503") || lower.contains("not loaded") || lower.contains("service unavailable")) {
            if (model.contains("Qwen 1.5B") || model.contains("CosmoChat Gateway")) {
                return prefix + "Локальная модель Qwen не загружена. Запустите Python API сервер и дождитесь полной загрузки модели.";
            } else if (model.contains("HuggingFace")) {
                return prefix + "Сервис HuggingFace недоступен. Модель загружается, попробуйте через 1-2 минуты.";
            } else if (model.contains("Mistral")) {
                return prefix + "Сервис Mistral недоступен. Проверьте настройки API.";
            } else if (model.contains("Deepseek")) {
                return prefix + "Сервис Deepseek недоступен. Проверьте настройки API.";
            } else {
                return prefix + "Сервис модели недоступен. Попробуйте позже.";
            }
        }

        // Rate limit (429)
        if (lower.contains("429") || lower.contains("rate limit")) {
            if (model.contains("HuggingFace")) {
                return prefix + "Превышен лимит запросов HuggingFace API. Пожалуйста, подождите несколько минут.";
            } else if (model.contains("Mistral")) {
                return prefix + "Превышен лимит запросов Mistral API. Подождите и попробуйте снова.";
            } else if (model.contains("Deepseek")) {
                return prefix + "Превышен лимит запросов Deepseek API. Подождите и попробуйте снова.";
            } else {
                return prefix + "Превышен лимит запросов. Пожалуйста, подождите минуту.";
            }
        }

        // Authentication errors (401, 403)
        if (lower.contains("401") || lower.contains("403") || lower.contains("unauthorized") || lower.contains("forbidden")) {
            if (model.contains("HuggingFace")) {
                return prefix + "Ошибка авторизации HuggingFace. Проверьте ваш API токен (HF_TOKEN в .env).";
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
            return prefix + "Неверный запрос к модели. Убедитесь, что модель существует и токен имеет доступ. Ошибка: " + msg;
        }

        // Server errors (500, 502, 504)
        if (lower.contains("500") || lower.contains("502") || lower.contains("504")) {
            if (model.contains("HuggingFace")) {
                return prefix + "Ошибка сервера HuggingFace. Попробуйте позже.";
            } else {
                return prefix + "Внутренняя ошибка сервера модели. Попробуйте позже.";
            }
        }

        // Default fallback — show full error
        return prefix + msg;
    }
}

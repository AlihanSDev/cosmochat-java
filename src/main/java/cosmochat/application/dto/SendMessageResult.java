package cosmochat.application.dto;

import cosmochat.domain.ChatId;
import cosmochat.domain.Message;

public record SendMessageResult(
    ChatId chatId,
    Message userMessage,
    Message aiMessage
) {}

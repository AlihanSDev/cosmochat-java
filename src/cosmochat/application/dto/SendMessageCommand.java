package cosmochat.application.dto;

import cosmochat.domain.ChatId;
import cosmochat.domain.Message;
import cosmochat.domain.UserId;

public record SendMessageCommand(
    UserId userId,
    ChatId chatId,
    String text
) {}

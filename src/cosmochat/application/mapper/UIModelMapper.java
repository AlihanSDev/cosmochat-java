package cosmochat.application.mapper;

import cosmochat.ChatItem;
import cosmochat.ChatMessage;
import cosmochat.domain.Chat;
import cosmochat.domain.Message;
import cosmochat.domain.Role;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UIModelMapper {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static ChatItem toChatItem(Chat chat) {
        return new ChatItem(chat.getId().getValue(), chat.getTitle(), "Ранее", "★");
    }

    public static ChatMessage toChatMessage(Message msg) {
        LocalTime time = LocalTime.ofInstant(
                Instant.ofEpochMilli(msg.getTimestamp()),
                ZoneId.systemDefault()
        );
        String timeStr = time.format(TIME_FORMATTER);
        ChatMessage.Role uiRole = msg.getRole() == Role.USER ? ChatMessage.Role.USER : ChatMessage.Role.AI;
        return new ChatMessage(uiRole, msg.getText(), timeStr);
    }
}

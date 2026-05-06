package cosmochat.application.mapper;

import cosmochat.ChatItem;
import cosmochat.ChatMessage;
import cosmochat.domain.Chat;
import cosmochat.domain.Message;
import cosmochat.domain.Role;
import cosmochat.application.dto.ChatDTO;
import cosmochat.application.dto.MessageDTO;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UIModelMapper {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // Domain → UI
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

    // DTO → UI (direct mapping, DTO already contains UI-ready data)
    public static ChatItem toChatItem(ChatDTO dto) {
        return new ChatItem(dto.id(), dto.title(), dto.date(), dto.iconGlyph());
    }

    public static ChatMessage toChatMessage(MessageDTO dto) {
        ChatMessage.Role uiRole = dto.role() == Role.USER ? ChatMessage.Role.USER : ChatMessage.Role.AI;
        return new ChatMessage(uiRole, dto.text(), dto.time());
    }

    // Factory methods for creating UI objects from raw data (used by Presentation layer)
    public static ChatItem createChatItem(int id, String title, String date, String iconGlyph) {
        return new ChatItem(id, title, date, iconGlyph);
    }

    public static ChatMessage createChatMessage(Role role, String text, String time) {
        ChatMessage.Role uiRole = role == Role.USER ? ChatMessage.Role.USER : ChatMessage.Role.AI;
        return new ChatMessage(uiRole, text, time);
    }
}

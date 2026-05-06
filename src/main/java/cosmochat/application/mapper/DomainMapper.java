package cosmochat.application.mapper;

import cosmochat.domain.User;
import cosmochat.domain.UserId;
import cosmochat.domain.Chat;
import cosmochat.domain.ChatId;
import cosmochat.domain.Message;
import cosmochat.domain.MessageId;
import cosmochat.domain.Role;
import cosmochat.application.dto.UserDTO;
import cosmochat.application.dto.ChatDTO;
import cosmochat.application.dto.MessageDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DomainMapper {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public static UserDTO toUserDTO(User user) {
        return new UserDTO(
            user.getId().getValue(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }

    public static ChatDTO toChatDTO(Chat chat) {
        return new ChatDTO(
            chat.getId().getValue(),
            chat.getTitle(),
            "Ранее",
            "★"
        );
    }

    public static MessageDTO toMessageDTO(Message message) {
        String time = LocalDateTime.ofEpochSecond(message.getTimestamp(), 0, java.time.ZoneOffset.UTC)
                .format(DATE_FORMATTER);
        return new MessageDTO(
            message.getRole(),
            message.getText(),
            time
        );
    }

    public static User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        long createdAt = rs.getTimestamp("created_at").getTime();
        return new User(new UserId(id), username, email, passwordHash, createdAt);
    }

    public static Chat mapResultSetToChat(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        String title = rs.getString("title");
        long createdAt = rs.getTimestamp("created_at").getTime();
        return new Chat(new ChatId(id), new UserId(userId), title, createdAt);
    }

    public static Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int chatId = rs.getInt("chat_id");
        String roleStr = rs.getString("role");
        Role role = Role.valueOf(roleStr);
        String text = rs.getString("text");
        String timestampStr = rs.getString("timestamp");
        long timestamp = parseTimestamp(timestampStr);
        return new Message(new MessageId(id), new ChatId(chatId), role, text, timestamp);
    }

    private static long parseTimestamp(String timestampStr) {
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }
}

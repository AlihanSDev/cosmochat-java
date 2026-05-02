package cosmochat.infrastructure.adapter;

import cosmochat.database.DatabaseManager;
import cosmochat.domain.ChatId;
import cosmochat.domain.Message;
import cosmochat.domain.MessageId;
import cosmochat.domain.Role;
import cosmochat.domain.port.MessageRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteMessageRepository implements MessageRepository {
    private final Connection connection;

    public SqliteMessageRepository() throws SQLException {
        this.connection = DatabaseManager.getInstance().getConnection();
        initialize();
    }

    private void initialize() {}

    @Override
    public Message save(Message message) {
        String sql = "INSERT INTO messages (chat_id, role, text, timestamp) VALUES (?, ?, ?, datetime('now'))";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, message.getChatId().getValue());
            stmt.setString(2, message.getRole().name());
            stmt.setString(3, message.getText());
            // timestamp TEXT - could store epoch or formatted time
            stmt.setString(4, formatTimestamp(message.getTimestamp()));
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        return new Message(new MessageId(id), message.getChatId(), message.getRole(), message.getText(), message.getTimestamp());
                    }
                }
            }
            throw new IllegalStateException("Failed to insert message");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Message> findByChatId(ChatId chatId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT id, chat_id, role, text, timestamp FROM messages WHERE chat_id = ? ORDER BY id ASC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chatId.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return messages;
    }

    @Override
    public void deleteByChatId(ChatId chatId) {
        String sql = "DELETE FROM messages WHERE chat_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chatId.getValue());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int chatId = rs.getInt("chat_id");
        Role role = Role.valueOf(rs.getString("role"));
        String text = rs.getString("text");
        String timestampStr = rs.getString("timestamp");
        long timestamp = parseTimestamp(timestampStr);
        return new Message(new MessageId(id), new ChatId(chatId), role, text, timestamp);
    }

    private String formatTimestamp(long epochMilli) {
        // For simplicity, store as ISO string or epoch millis as string
        return String.valueOf(epochMilli);
    }

    private long parseTimestamp(String timestampStr) {
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }
}

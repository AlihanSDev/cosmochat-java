package cosmochat.database;

import cosmochat.ChatMessage;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private final DatabaseManager dbManager;

    public MessageDAO() throws SQLException {
        this.dbManager = DatabaseManager.getInstance();
    }

    public void addMessage(int chatId, ChatMessage.Role role, String text, String timestamp) throws SQLException {
        String sql = "INSERT INTO messages (chat_id, role, text, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            stmt.setString(2, role.name());
            stmt.setString(3, text);
            stmt.setString(4, timestamp);
            stmt.executeUpdate();
        }

        // Update chat's updated_at timestamp
        String updateChat = "UPDATE chats SET updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(updateChat)) {
            stmt.setInt(1, chatId);
            stmt.executeUpdate();
        }
    }

    public List<ChatMessage> getMessagesForChat(int chatId) throws SQLException {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT role, text, timestamp FROM messages WHERE chat_id = ? ORDER BY id ASC";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ChatMessage.Role role = ChatMessage.Role.valueOf(rs.getString("role"));
                    String text = rs.getString("text");
                    String timestamp = rs.getString("timestamp");
                    messages.add(new ChatMessage(role, text, timestamp));
                }
            }
        }
        return messages;
    }

    public void clearMessagesForChat(int chatId) throws SQLException {
        String sql = "DELETE FROM messages WHERE chat_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            stmt.executeUpdate();
        }
    }
}

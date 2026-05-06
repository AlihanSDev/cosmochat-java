package cosmochat.database;

import cosmochat.ChatItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ChatDAO {
    private final DatabaseManager dbManager;

    public ChatDAO() throws SQLException {
        this.dbManager = DatabaseManager.getInstance();
    }

    public ChatItem createChat(int userId, String title) throws SQLException {
        String sql = "INSERT INTO chats (user_id, title) VALUES (?, ?)";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, userId);
            stmt.setString(2, title);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    return new ChatItem(id, title, "Сейчас", "★");
                }
            }
        }
        return null;
    }

    public List<ChatItem> getChatsForUser(int userId) throws SQLException {
        List<ChatItem> chats = new ArrayList<>();
        String sql = "SELECT id, title, created_at FROM chats WHERE user_id = ? ORDER BY updated_at DESC";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String title = rs.getString("title");
                    chats.add(new ChatItem(id, title, "Ранее", "★"));
                }
            }
        }
        return chats;
    }

    public void updateChatTitle(int chatId, String newTitle) throws SQLException {
        String sql = "UPDATE chats SET title = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, newTitle);
            stmt.setInt(2, chatId);
            stmt.executeUpdate();
        }
    }

    public void deleteChat(int chatId) throws SQLException {
        // Messages will be cascade-deleted by foreign key
        String sql = "DELETE FROM chats WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            stmt.executeUpdate();
        }
    }

    public Optional<Integer> getChatOwner(int chatId) throws SQLException {
        String sql = "SELECT user_id FROM chats WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, chatId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getInt("user_id"));
                }
            }
        }
        return Optional.empty();
    }
}

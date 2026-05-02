package cosmochat.infrastructure.adapter;

import cosmochat.database.DatabaseManager;
import cosmochat.domain.Chat;
import cosmochat.domain.ChatId;
import cosmochat.domain.UserId;
import cosmochat.domain.port.ChatRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteChatRepository implements ChatRepository {
    private final Connection connection;

    public SqliteChatRepository() throws SQLException {
        this.connection = DatabaseManager.getInstance().getConnection();
        initialize();
    }

    private void initialize() {}

    @Override
    public Chat save(Chat chat) {
        if (chat.getId().getValue() == 0) {
            // Insert
            String sql = "INSERT INTO chats (user_id, title, created_at, updated_at) VALUES (?, ?, datetime('now'), datetime('now'))";
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, chat.getUserId().getValue());
                stmt.setString(2, chat.getTitle());
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int id = keys.getInt(1);
                            return new Chat(new ChatId(id), chat.getUserId(), chat.getTitle(), chat.getCreatedAt());
                        }
                    }
                }
                throw new IllegalStateException("Failed to insert chat");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Update
            String sql = "UPDATE chats SET title = ?, updated_at = datetime('now') WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, chat.getTitle());
                stmt.setInt(2, chat.getId().getValue());
                stmt.executeUpdate();
                return chat;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<Chat> findByUserId(UserId userId) {
        List<Chat> chats = new ArrayList<>();
        String sql = "SELECT id, user_id, title, created_at FROM chats WHERE user_id = ? ORDER BY updated_at DESC";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    chats.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return chats;
    }

    @Override
    public Optional<Chat> findById(ChatId chatId) {
        String sql = "SELECT id, user_id, title, created_at FROM chats WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chatId.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public void delete(ChatId chatId) {
        String sql = "DELETE FROM chats WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chatId.getValue());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateTitle(ChatId chatId, String newTitle) {
        String sql = "UPDATE chats SET title = ?, updated_at = datetime('now') WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newTitle);
            stmt.setInt(2, chatId.getValue());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<UserId> findOwner(ChatId chatId) {
        String sql = "SELECT user_id FROM chats WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, chatId.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new UserId(rs.getInt("user_id")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    private Chat mapRow(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        String title = rs.getString("title");
        long createdAt = rs.getTimestamp("created_at").getTime();
        return new Chat(new ChatId(id), new UserId(userId), title, createdAt);
    }
}

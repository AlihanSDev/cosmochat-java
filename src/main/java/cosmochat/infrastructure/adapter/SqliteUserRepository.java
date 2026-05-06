package cosmochat.infrastructure.adapter;

import cosmochat.database.DatabaseManager;
import cosmochat.domain.User;
import cosmochat.domain.UserId;
import cosmochat.domain.port.UserRepository;
import cosmochat.application.mapper.DomainMapper;

import java.sql.*;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {
    private final Connection connection;

    public SqliteUserRepository() throws SQLException {
        this.connection = DatabaseManager.getInstance().getConnection();
        initialize();
    }

    private void initialize() {
        // Schema already created by DatabaseManager
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT id, username, email, password_hash, created_at FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(DomainMapper.mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        if (user.getId().getValue() == 0) {
            // Insert
            String sql = "INSERT INTO users (username, email, password_hash, created_at) VALUES (?, ?, ?, datetime('now'))";
            try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getPasswordHash());
                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int id = keys.getInt(1);
                            return new User(new UserId(id), user.getUsername(), user.getEmail(), user.getPasswordHash(), user.getCreatedAt());
                        }
                    }
                }
                throw new IllegalStateException("Failed to insert user");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Update
            String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getEmail());
                stmt.setInt(3, user.getId().getValue());
                stmt.executeUpdate();
                return user;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public User findById(UserId userId) {
        String sql = "SELECT id, username, email, password_hash, created_at FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return DomainMapper.mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

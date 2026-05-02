package cosmochat.database;

import cosmochat.model.User;
import cosmochat.security.PasswordHasher;

import java.sql.*;
import java.util.Optional;

public class UserDAO {
    private final DatabaseManager dbManager;

    public UserDAO() throws SQLException {
        this.dbManager = DatabaseManager.getInstance();
    }

    public boolean userExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Optional<User> register(String username, String email, String password) throws Exception {
        if (userExists(email)) {
            return Optional.empty(); // Email already taken
        }

        String passwordHash = PasswordHasher.hashPassword(password);
        String sql = "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, passwordHash);
            int affected = stmt.executeUpdate();

            if (affected > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        return Optional.of(new User(id, username, email, System.currentTimeMillis()));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> authenticate(String email, String password) throws Exception {
        String sql = "SELECT id, username, email, password_hash, created_at FROM users WHERE email = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = mapResultSetToUser(rs);
                    String storedHash = user.getPasswordHash();
                    if (PasswordHasher.verifyPassword(password, storedHash)) {
                        // Return user without password hash for session
                        return Optional.of(new User(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            user.getCreatedAt()
                        ));
                    }
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, created_at FROM users WHERE email = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, created_at FROM users WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(sql)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String username = rs.getString("username");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        long createdAt = rs.getTimestamp("created_at").getTime();
        return new User(id, username, email, passwordHash, createdAt);
    }
}

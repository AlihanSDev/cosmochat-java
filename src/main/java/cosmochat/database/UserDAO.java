package cosmochat.database;

import cosmochat.domain.User;
import cosmochat.domain.UserId;
import cosmochat.application.mapper.DomainMapper;
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
                        return Optional.of(new User(new UserId(id), username, email, passwordHash, System.currentTimeMillis()));
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
                    User user = DomainMapper.mapResultSetToUser(rs);
                    String storedHash = user.getPasswordHash();
                    if (PasswordHasher.verifyPassword(password, storedHash)) {
                        // Return user without password hash for session
                        return Optional.of(new User(
                            user.getId(),
                            user.getUsername(),
                            user.getEmail(),
                            null, // password hash not needed after login
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
                    return Optional.of(DomainMapper.mapResultSetToUser(rs));
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
                    return DomainMapper.mapResultSetToUser(rs);
                }
            }
        }
        return null;
    }
}

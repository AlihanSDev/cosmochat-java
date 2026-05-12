package cosmochat.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static DatabaseManager instance;
    private final Connection connection;

    private DatabaseManager() throws SQLException {
        try {
            // Load PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver not found", e);
        }

        String url;
        
        // Check if we're in test mode (for CI/testing)
        String testMode = System.getProperty("cosmochat.test.mode");
        if ("h2".equals(testMode)) {
            // Use H2 in-memory database for tests (faster, no file I/O)
            try {
                Class.forName("org.h2.Driver");
                url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL";
                this.connection = DriverManager.getConnection(url);
                initializeSchema();
                return;
            } catch (ClassNotFoundException e) {
                throw new SQLException("H2 driver not found for test mode", e);
            }
        }

        // Production mode: PostgreSQL (configure via env vars or system properties)
        url = getConfigValue("COSMOCHAT_DB_URL", "cosmochat.db.url", "jdbc:postgresql://localhost:5432/cosmochat");
        String user = getConfigValue("COSMOCHAT_DB_USER", "cosmochat.db.user", null);
        String password = getConfigValue("COSMOCHAT_DB_PASSWORD", "cosmochat.db.password", null);

        if (user != null && password != null) {
            this.connection = DriverManager.getConnection(url, user, password);
        } else if (user != null) {
            this.connection = DriverManager.getConnection(url, user, "");
        } else {
            this.connection = DriverManager.getConnection(url);
        }
        this.connection.setAutoCommit(true);
        initializeSchema();
    }

    private String getConfigValue(String envKey, String sysPropKey, String defaultValue) {
        String fromSysProp = System.getProperty(sysPropKey);
        if (fromSysProp != null && !fromSysProp.isBlank()) {
            return fromSysProp.trim();
        }
        String fromEnv = System.getenv(envKey);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        return defaultValue;
    }

    private void initializeSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Users table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id SERIAL PRIMARY KEY,
                    username TEXT NOT NULL UNIQUE,
                    email TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Chats table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS chats (
                    id SERIAL PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // Messages table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS messages (
                    id SERIAL PRIMARY KEY,
                    chat_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    text TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE
                )
            """);

            // Sessions table for persistent login (optional)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    expires_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);

            // User usage tracking for rate limiting
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_usage (
                    user_id INTEGER PRIMARY KEY,
                    messages_sent INTEGER NOT NULL DEFAULT 0,
                    hour_window_start TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
            """);
        }
    }

    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

package cosmochat.database;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UsageStatsDAO {
    private static final int HOURLY_MESSAGE_LIMIT = 100;
    // SQLite CURRENT_TIMESTAMP format: "yyyy-MM-dd HH:mm:ss"
    private static final DateTimeFormatter SQLITE_DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DatabaseManager dbManager;

    public UsageStatsDAO() throws SQLException {
        this.dbManager = DatabaseManager.getInstance();
    }

    public static int getHourlyMessageLimit() {
        return HOURLY_MESSAGE_LIMIT;
    }

    public UsageStats getOrCreateUsage(int userId) throws SQLException {
        String select = "SELECT messages_sent, hour_window_start FROM user_usage WHERE user_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(select)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String windowStartStr = rs.getString("hour_window_start");
                    LocalDateTime windowStart = LocalDateTime.parse(windowStartStr, SQLITE_DATETIME_FORMATTER);
                    int messagesSent = rs.getInt("messages_sent");
                    return new UsageStats(userId, messagesSent, windowStart);
                }
            }
        }
        // Create new usage record
        String insert = "INSERT INTO user_usage (user_id, messages_sent, hour_window_start) VALUES (?, 0, CURRENT_TIMESTAMP)";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(insert)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
        return new UsageStats(userId, 0, LocalDateTime.now());
    }

    public boolean canSendMessage(int userId) throws SQLException {
        UsageStats stats = getOrCreateUsage(userId);
        if (stats.isWindowExpired()) {
            // Reset the window
            resetUsage(userId);
            return true;
        }
        return stats.getMessagesSent() < HOURLY_MESSAGE_LIMIT;
    }

    public void incrementMessageCount(int userId) throws SQLException {
        UsageStats stats = getOrCreateUsage(userId);
        if (stats.isWindowExpired()) {
            resetUsage(userId);
            stats = getOrCreateUsage(userId);
        }
        String update = "UPDATE user_usage SET messages_sent = messages_sent + 1, hour_window_start = ? WHERE user_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(update)) {
            stmt.setString(1, LocalDateTime.now().format(SQLITE_DATETIME_FORMATTER));
            stmt.setInt(2, userId);
            stmt.executeUpdate();
        }
    }

    public void resetUsage(int userId) throws SQLException {
        String reset = "UPDATE user_usage SET messages_sent = 0, hour_window_start = CURRENT_TIMESTAMP WHERE user_id = ?";
        try (PreparedStatement stmt = dbManager.getConnection().prepareStatement(reset)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        }
    }

    public UsageStats getCurrentStats(int userId) throws SQLException {
        return getOrCreateUsage(userId);
    }

    public static class UsageStats {
        private final int userId;
        private final int messagesSent;
        private final LocalDateTime windowStart;

        public UsageStats(int userId, int messagesSent, LocalDateTime windowStart) {
            this.userId = userId;
            this.messagesSent = messagesSent;
            this.windowStart = windowStart;
        }

        public int getMessagesSent() {
            return messagesSent;
        }

        public int getRemaining() {
            return Math.max(0, HOURLY_MESSAGE_LIMIT - messagesSent);
        }

        public LocalDateTime getWindowStart() {
            return windowStart;
        }

        public boolean isWindowExpired() {
            LocalDateTime now = LocalDateTime.now();
            return now.minusHours(1).isAfter(windowStart);
        }

        public double getUsagePercentage() {
            return ((double) messagesSent / HOURLY_MESSAGE_LIMIT) * 100;
        }

        public LocalDateTime getWindowEnd() {
            return windowStart.plusHours(1);
        }
    }
}

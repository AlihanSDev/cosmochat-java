package cosmochat.infrastructure.adapter;

import cosmochat.application.dto.UsageStats;
import cosmochat.application.port.UsageTracking;
import cosmochat.domain.UserId;
import cosmochat.database.UsageStatsDAO;

import java.sql.SQLException;

public class UsageTrackingAdapter implements UsageTracking {
    private final UsageStatsDAO usageStatsDAO;

    public UsageTrackingAdapter() throws SQLException {
        this.usageStatsDAO = new UsageStatsDAO();
    }

    @Override
    public boolean canSendMessage(UserId userId) {
        try {
            return usageStatsDAO.canSendMessage(userId.getValue());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check rate limit", e);
        }
    }

    @Override
    public void incrementMessageCount(UserId userId) {
        try {
            usageStatsDAO.incrementMessageCount(userId.getValue());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to increment message count", e);
        }
    }

    @Override
    public UsageStats getCurrentStats(UserId userId) {
        try {
            UsageStatsDAO.UsageStats stats = usageStatsDAO.getCurrentStats(userId.getValue());
            return new UsageStats(
                userId,
                stats.getMessagesSent(),
                UsageStatsDAO.getHourlyMessageLimit(),
                stats.getWindowStart(),
                stats.getWindowEnd()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get usage stats", e);
        }
    }

    @Override
    public void resetUsageLimits(UserId userId) {
        try {
            usageStatsDAO.resetUsage(userId.getValue());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset usage limits", e);
        }
    }
}

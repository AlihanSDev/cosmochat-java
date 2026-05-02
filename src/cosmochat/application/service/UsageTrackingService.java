package cosmochat.application.service;

import cosmochat.application.port.UsageTracking;
import cosmochat.application.dto.UsageStats;
import cosmochat.domain.UserId;
import cosmochat.database.UsageStatsDAO;

import java.sql.SQLException;

public class UsageTrackingService implements UsageTracking {
    private final UsageStatsDAO usageStatsDAO;

    public UsageTrackingService(UsageStatsDAO usageStatsDAO) {
        this.usageStatsDAO = usageStatsDAO;
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
                100,
                stats.getWindowStart(),
                stats.getWindowEnd()
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get usage stats", e);
        }
    }
}

package cosmochat.application.port;

import cosmochat.domain.UserId;
import cosmochat.application.dto.UsageStats;

public interface UsageTracking {
    boolean canSendMessage(UserId userId);
    void incrementMessageCount(UserId userId);
    UsageStats getCurrentStats(UserId userId);
}

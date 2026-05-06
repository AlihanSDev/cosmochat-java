package cosmochat.application.dto;

import cosmochat.domain.UserId;

public record UsageStats(
    UserId userId,
    int messagesSent,
    int hourlyLimit,
    java.time.LocalDateTime windowStart,
    java.time.LocalDateTime windowEnd
) {
    public double getUsagePercentage() {
        return (double) messagesSent / hourlyLimit * 100;
    }

    public boolean isWindowExpired() {
        return java.time.LocalDateTime.now().isAfter(windowEnd);
    }
}

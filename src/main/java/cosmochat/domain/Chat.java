package cosmochat.domain;

public final class Chat {
    private final ChatId id;
    private final UserId userId;
    private final String title;
    private final long createdAt;

    public Chat(ChatId id, UserId userId, String title, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.createdAt = createdAt;
    }

    public ChatId getId() {
        return id;
    }

    public UserId getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

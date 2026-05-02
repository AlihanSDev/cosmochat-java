package cosmochat.domain;

public final class Message {
    private final MessageId id;
    private final ChatId chatId;
    private final Role role;
    private final String text;
    private final long timestamp;

    public Message(MessageId id, ChatId chatId, Role role, String text, long timestamp) {
        this.id = id;
        this.chatId = chatId;
        this.role = role;
        this.text = text;
        this.timestamp = timestamp;
    }

    public MessageId getId() {
        return id;
    }

    public ChatId getChatId() {
        return chatId;
    }

    public Role getRole() {
        return role;
    }

    public String getText() {
        return text;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

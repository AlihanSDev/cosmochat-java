package cosmochat.domain;

import java.util.Objects;

public final class ChatId {
    private final int value;

    public ChatId(int value) {
        if (value < 0) {
            throw new IllegalArgumentException("Chat ID cannot be negative");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatId chatId = (ChatId) o;
        return value == chatId.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}

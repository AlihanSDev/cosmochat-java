package cosmochat.domain;

import java.util.Objects;

public final class MessageId {
    private final int value;

    public MessageId(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException("Message ID must be positive");
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
        MessageId messageId = (MessageId) o;
        return value == messageId.value;
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

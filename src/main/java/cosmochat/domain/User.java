package cosmochat.domain;

public final class User {
    private final UserId id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final long createdAt;

    public User(UserId id, String username, String email, String passwordHash, long createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public UserId getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

package cosmochat.model;

public class User {
    private final int id;
    private final String username;
    private final String email;
    private final long createdAt;

    public User(int id, String username, String email, long createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}

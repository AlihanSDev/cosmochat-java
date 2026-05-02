package cosmochat.application.dto;

public record UserDTO(
    int id,
    String username,
    String email,
    long createdAt
) {}

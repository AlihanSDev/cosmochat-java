package cosmochat.application.dto;

public record AuthenticateUserCommand(
    String email,
    String password
) {}

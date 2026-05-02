package cosmochat.application.dto;

import cosmochat.domain.Role;

public record MessageDTO(
    Role role,
    String text,
    String time
) {}

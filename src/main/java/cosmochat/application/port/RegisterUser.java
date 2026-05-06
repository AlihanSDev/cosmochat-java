package cosmochat.application.port;

import cosmochat.domain.User;
import cosmochat.application.dto.RegisterUserCommand;

import java.util.Optional;

public interface RegisterUser {
    Optional<User> execute(RegisterUserCommand command);
}

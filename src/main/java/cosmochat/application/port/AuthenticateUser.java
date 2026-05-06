package cosmochat.application.port;

import cosmochat.domain.User;
import cosmochat.application.dto.AuthenticateUserCommand;

import java.util.Optional;

public interface AuthenticateUser {
    Optional<User> execute(AuthenticateUserCommand command);
}

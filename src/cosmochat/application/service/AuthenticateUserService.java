package cosmochat.application.service;

import cosmochat.application.port.AuthenticateUser;
import cosmochat.application.dto.AuthenticateUserCommand;
import cosmochat.domain.User;
import cosmochat.domain.UserId;
import cosmochat.domain.port.UserRepository;
import cosmochat.security.PasswordHasher;

import java.util.Optional;

public class AuthenticateUserService implements AuthenticateUser {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticateUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<User> execute(AuthenticateUserCommand command) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(command.email());
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (passwordHasher.verifyPassword(command.password(), user.getPasswordHash())) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Authentication failed", e);
        }
    }
}

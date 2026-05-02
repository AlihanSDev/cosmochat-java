package cosmochat.application.service;

import cosmochat.application.port.RegisterUser;
import cosmochat.application.dto.RegisterUserCommand;
import cosmochat.domain.User;
import cosmochat.domain.UserId;
import cosmochat.domain.port.UserRepository;
import cosmochat.security.PasswordHasher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class RegisterUserService implements RegisterUser {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public Optional<User> execute(RegisterUserCommand command) {
        try {
            if (userRepository.existsByEmail(command.email())) {
                return Optional.empty();
            }

            String passwordHash = passwordHasher.hashPassword(command.password());
            long now = Instant.now().toEpochMilli();
            User user = new User(
                new UserId(0), // Will be replaced after save
                command.username(),
                command.email(),
                passwordHash,
                now
            );

            User savedUser = userRepository.save(user);
            return Optional.of(savedUser);
        } catch (Exception e) {
            throw new RuntimeException("Registration failed", e);
        }
    }
}

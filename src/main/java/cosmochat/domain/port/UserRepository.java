package cosmochat.domain.port;

import cosmochat.domain.User;
import cosmochat.domain.UserId;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByEmail(String email);
    User save(User user);
    User findById(UserId userId);
    boolean existsByEmail(String email);
}

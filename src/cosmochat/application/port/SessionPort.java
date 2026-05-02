package cosmochat.application.port;

import cosmochat.domain.User;

public interface SessionPort {
    void login(User user);
    void logout();
    User getCurrentUser();
    boolean isLoggedIn();
}

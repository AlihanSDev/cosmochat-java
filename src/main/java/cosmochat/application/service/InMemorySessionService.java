package cosmochat.application.service;

import cosmochat.application.port.SessionPort;
import cosmochat.domain.User;

public class InMemorySessionService implements SessionPort {
    private static InMemorySessionService instance;
    private User currentUser;

    private InMemorySessionService() {}

    public static synchronized InMemorySessionService getInstance() {
        if (instance == null) {
            instance = new InMemorySessionService();
        }
        return instance;
    }

    @Override
    public void login(User user) {
        this.currentUser = user;
    }

    @Override
    public void logout() {
        this.currentUser = null;
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }

    @Override
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}

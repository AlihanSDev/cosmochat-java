package cosmochat.application.service;

import cosmochat.application.port.GetCurrentUser;
import cosmochat.application.port.SessionPort;
import cosmochat.domain.User;

public class GetCurrentUserService implements GetCurrentUser {
    private final SessionPort session;

    public GetCurrentUserService(SessionPort session) {
        this.session = session;
    }

    @Override
    public User execute() {
        return session.getCurrentUser();
    }
}

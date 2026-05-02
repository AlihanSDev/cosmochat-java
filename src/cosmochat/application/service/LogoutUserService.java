package cosmochat.application.service;

import cosmochat.application.port.LogoutUser;
import cosmochat.application.port.SessionPort;

public class LogoutUserService implements LogoutUser {
    private final SessionPort session;

    public LogoutUserService(SessionPort session) {
        this.session = session;
    }

    @Override
    public void execute() {
        session.logout();
    }
}

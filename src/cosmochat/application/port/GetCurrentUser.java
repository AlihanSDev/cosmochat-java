package cosmochat.application.port;

import cosmochat.domain.UserId;

public interface GetCurrentUser {
    cosmochat.domain.User execute();
}

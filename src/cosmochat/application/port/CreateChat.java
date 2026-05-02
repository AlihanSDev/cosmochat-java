package cosmochat.application.port;

import cosmochat.domain.Chat;
import cosmochat.domain.UserId;
import cosmochat.application.dto.CreateChatCommand;

public interface CreateChat {
    Chat execute(CreateChatCommand command);
    Chat executeForUser(UserId userId, String title);
}

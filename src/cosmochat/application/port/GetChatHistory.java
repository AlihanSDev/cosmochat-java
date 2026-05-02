package cosmochat.application.port;

import cosmochat.domain.Chat;
import cosmochat.domain.UserId;

import java.util.List;

public interface GetChatHistory {
    List<Chat> execute(UserId userId);
}

package cosmochat.application.port;

import cosmochat.domain.ChatId;

public interface DeleteChat {
    boolean execute(ChatId chatId);
}

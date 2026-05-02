package cosmochat.application.port;

import cosmochat.domain.ChatId;
import cosmochat.domain.Message;

import java.util.List;

public interface GetChatMessages {
    List<Message> execute(ChatId chatId);
}

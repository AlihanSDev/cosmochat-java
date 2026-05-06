package cosmochat.domain.port;

import cosmochat.domain.ChatId;
import cosmochat.domain.Message;
import cosmochat.domain.Role;

import java.util.List;

public interface MessageRepository {
    Message save(Message message);
    List<Message> findByChatId(ChatId chatId);
    void deleteByChatId(ChatId chatId);
}

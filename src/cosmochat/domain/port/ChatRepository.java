package cosmochat.domain.port;

import cosmochat.domain.Chat;
import cosmochat.domain.ChatId;
import cosmochat.domain.UserId;

import java.util.List;
import java.util.Optional;

public interface ChatRepository {
    Chat save(Chat chat);
    List<Chat> findByUserId(UserId userId);
    Optional<Chat> findById(ChatId chatId);
    void delete(ChatId chatId);
    void updateTitle(ChatId chatId, String newTitle);
    Optional<UserId> findOwner(ChatId chatId);
}

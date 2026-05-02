package cosmochat.application.service;

import cosmochat.application.port.GetChatHistory;
import cosmochat.domain.Chat;
import cosmochat.domain.UserId;
import cosmochat.domain.port.ChatRepository;

import java.util.List;

public class GetChatHistoryService implements GetChatHistory {
    private final ChatRepository chatRepository;

    public GetChatHistoryService(ChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @Override
    public List<Chat> execute(UserId userId) {
        return chatRepository.findByUserId(userId);
    }
}

package cosmochat.application.service;

import cosmochat.application.port.CreateChat;
import cosmochat.application.dto.CreateChatCommand;
import cosmochat.domain.Chat;
import cosmochat.domain.ChatId;
import cosmochat.domain.UserId;
import cosmochat.domain.port.ChatRepository;
import cosmochat.domain.port.UserRepository;

import java.time.Instant;

public class CreateChatService implements CreateChat {
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public CreateChatService(ChatRepository chatRepository, UserRepository userRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Chat execute(CreateChatCommand command) {
        // For now, we'll require userId from context (session). In real app, command would include it.
        throw new UnsupportedOperationException("CreateChat requires userId from session. Use CreateChatForUser instead.");
    }

    public Chat executeForUser(UserId userId, String title) {
        Chat chat = new Chat(
            new ChatId(0), // Will be replaced after save
            userId,
            title,
            Instant.now().toEpochMilli()
        );
        return chatRepository.save(chat);
    }
}

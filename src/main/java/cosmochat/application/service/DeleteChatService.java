package cosmochat.application.service;

import cosmochat.application.port.DeleteChat;
import cosmochat.application.port.GetCurrentUser;
import cosmochat.domain.ChatId;
import cosmochat.domain.User;
import cosmochat.domain.UserId;
import cosmochat.domain.port.ChatRepository;
import cosmochat.domain.port.MessageRepository;

public class DeleteChatService implements DeleteChat {
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final GetCurrentUser getCurrentUser;

    public DeleteChatService(ChatRepository chatRepository, MessageRepository messageRepository, GetCurrentUser getCurrentUser) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
        this.getCurrentUser = getCurrentUser;
    }

    @Override
    public boolean execute(ChatId chatId) {
        User user = getCurrentUser.execute();
        if (user == null) {
            return false;
        }
        UserId currentUserId = user.getId();

        // Verify ownership
        var ownerOpt = chatRepository.findOwner(chatId);
        if (ownerOpt.isEmpty() || !ownerOpt.get().equals(currentUserId)) {
            return false; // Not authorized or chat doesn't exist
        }

        // Delete messages first (redundant if cascade works, but safe)
        messageRepository.deleteByChatId(chatId);

        // Delete chat
        chatRepository.delete(chatId);

        return true;
    }
}

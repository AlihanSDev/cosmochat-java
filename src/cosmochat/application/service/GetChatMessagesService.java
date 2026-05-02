package cosmochat.application.service;

import cosmochat.application.port.GetChatMessages;
import cosmochat.domain.ChatId;
import cosmochat.domain.Message;
import cosmochat.domain.port.MessageRepository;

import java.util.List;

public class GetChatMessagesService implements GetChatMessages {
    private final MessageRepository messageRepository;

    public GetChatMessagesService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Override
    public List<Message> execute(ChatId chatId) {
        return messageRepository.findByChatId(chatId);
    }
}

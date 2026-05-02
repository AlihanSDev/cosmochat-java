package cosmochat.application.service;

import cosmochat.application.port.SendMessage;
import cosmochat.application.port.UsageTracking;
import cosmochat.application.dto.SendMessageCommand;
import cosmochat.application.dto.SendMessageResult;
import cosmochat.domain.*;
import cosmochat.domain.port.*;

import java.time.Instant;

public class SendMessageService implements SendMessage {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final AiPort aiPort;
    private final UsageTracking usageTracking;

    public SendMessageService(
            MessageRepository messageRepository,
            ChatRepository chatRepository,
            UserRepository userRepository,
            AiPort aiPort,
            UsageTracking usageTracking
    ) {
        this.messageRepository = messageRepository;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.aiPort = aiPort;
        this.usageTracking = usageTracking;
    }

    @Override
    public SendMessageResult execute(SendMessageCommand command) throws Exception {
        UserId userId = command.userId();
        ChatId chatId = command.chatId();
        String text = command.text();

        // Validate user
        User user = userRepository.findById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Check rate limit
        if (!usageTracking.canSendMessage(userId)) {
            throw new IllegalStateException("Rate limit exceeded (100 messages/hour)");
        }

        // Determine chat: if null, create new
        Chat chat;
        if (chatId == null) {
            String title = text.length() > 30 ? text.substring(0, 30) + "..." : text;
            chat = chatRepository.save(new Chat(
                    new ChatId(0),
                    userId,
                    title,
                    Instant.now().toEpochMilli()
            ));
        } else {
            chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new IllegalArgumentException("Chat not found"));
        }

        // Save user message
        Message userMessage = new Message(
                new MessageId(0),
                chat.getId(),
                Role.USER,
                text,
                Instant.now().toEpochMilli()
        );
        messageRepository.save(userMessage);

        // Call AI
        String aiResponse = aiPort.sendMessage(text);

        // Save AI message
        Message aiMessage = new Message(
                new MessageId(0),
                chat.getId(),
                Role.AI,
                aiResponse,
                Instant.now().toEpochMilli()
        );
        messageRepository.save(aiMessage);

        // Increment usage
        usageTracking.incrementMessageCount(userId);

        return new SendMessageResult(chat.getId(), userMessage, aiMessage);
    }
}

package cosmochat.application.service;

import cosmochat.application.port.*;
import cosmochat.application.dto.*;
import cosmochat.domain.*;
import cosmochat.domain.port.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application Service — orchestrates use cases for the UI.
 * ChatController delegates all business logic here.
 * Returns DTOs (data transfer objects) that are independent of UI framework.
 */
public class ChatService {
    private final AuthenticateUser authenticateUser;
    private final RegisterUser registerUser;
    private final CreateChat createChat;
    private final GetChatHistory getChatHistory;
    private final GetChatMessages getChatMessages;
    private final SendMessage sendMessage;
    private final GetCurrentUser getCurrentUser;
    private final LogoutUser logoutUser;
    private final UsageTracking usageTracking;
    private final SessionPort session;
    private final DeleteChat deleteChat;

    public ChatService(
            AuthenticateUser authenticateUser,
            RegisterUser registerUser,
            CreateChat createChat,
            GetChatHistory getChatHistory,
            GetChatMessages getChatMessages,
            SendMessage sendMessage,
            GetCurrentUser getCurrentUser,
            LogoutUser logoutUser,
            UsageTracking usageTracking,
            SessionPort session,
            DeleteChat deleteChat) {
        this.authenticateUser = authenticateUser;
        this.registerUser = registerUser;
        this.createChat = createChat;
        this.getChatHistory = getChatHistory;
        this.getChatMessages = getChatMessages;
        this.sendMessage = sendMessage;
        this.getCurrentUser = getCurrentUser;
        this.logoutUser = logoutUser;
        this.usageTracking = usageTracking;
        this.session = session;
        this.deleteChat = deleteChat;
    }

    // Authentication
    public boolean login(String email, String password) {
        AuthenticateUserCommand cmd = new AuthenticateUserCommand(email, password);
        Optional<User> userOpt = authenticateUser.execute(cmd);
        if (userOpt.isPresent()) {
            session.login(userOpt.get());
            return true;
        }
        return false;
    }

    public boolean register(String username, String email, String password) {
        RegisterUserCommand cmd = new RegisterUserCommand(username, email, password);
        Optional<User> userOpt = registerUser.execute(cmd);
        if (userOpt.isPresent()) {
            session.login(userOpt.get());
            return true;
        }
        return false;
    }

    public UserDTO getCurrentUser() {
        User user = getCurrentUser.execute();
        if (user == null) return null;
        return new UserDTO(
            user.getId().getValue(),
            user.getUsername(),
            user.getEmail(),
            user.getCreatedAt()
        );
    }

    public void logout() {
        logoutUser.execute();
    }

    public boolean isLoggedIn() {
        return getCurrentUser.execute() != null;
    }

    // Chat
    public List<ChatDTO> getChatsForCurrentUser() {
        User user = getCurrentUser.execute();
        if (user == null) return List.of();
        List<Chat> chats = getChatHistory.execute(user.getId());
        return chats.stream()
                .map(chat -> new ChatDTO(
                    chat.getId().getValue(),
                    chat.getTitle(),
                    "Ранее",
                    "★"
                ))
                .collect(Collectors.toList());
    }

    public List<MessageDTO> getMessagesForChat(int chatId) {
        List<Message> messages = getChatMessages.execute(new ChatId(chatId));
        return messages.stream()
                .map(msg -> {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(msg.getTimestamp()),
                        java.time.ZoneId.systemDefault()
                    );
                    String time = ldt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    return new MessageDTO(
                        msg.getRole(),
                        msg.getText(),
                        time
                    );
                })
                .collect(Collectors.toList());
    }

    public int createChat(String title, int userId) {
        Chat chat = createChat.executeForUser(new UserId(userId), title);
        return chat.getId().getValue();
    }

    public boolean deleteChat(int chatId) {
        return deleteChat.execute(new ChatId(chatId));
    }

    // Rate limiting
    public boolean canSendMessage() {
        User user = getCurrentUser.execute();
        if (user == null) return false;
        return usageTracking.canSendMessage(user.getId());
    }

    public UsageStats getUsageStats() {
        User user = getCurrentUser.execute();
        if (user == null) return null;
        return usageTracking.getCurrentStats(user.getId());
    }

    public void resetUsageLimits() {
        User user = getCurrentUser.execute();
        if (user != null) {
            usageTracking.resetUsageLimits(user.getId());
        }
    }

    // Send message using command (with model)
    public SendMessageResult sendMessage(SendMessageCommand command) throws Exception {
        return sendMessage.execute(command);
    }

    // Send message (legacy, kept for backwards compatibility)
    public SendMessageResult sendMessage(int chatId, String text) throws Exception {
        User user = getCurrentUser.execute();
        if (user == null) throw new IllegalStateException("Not authenticated");

        SendMessageCommand cmd = new SendMessageCommand(
            user.getId(),
            chatId == 0 ? null : new ChatId(chatId),
            text,
            "Qwen 1.5B Coder (CosmoChat Gateway)" // default model
        );
        return sendMessage.execute(cmd);
    }
}

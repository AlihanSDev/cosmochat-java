package cosmochat.infrastructure;

import cosmochat.application.port.*;
import cosmochat.application.service.*;
import cosmochat.domain.port.*;
import cosmochat.infrastructure.adapter.*;
import cosmochat.database.DatabaseManager;

import java.sql.SQLException;
import java.util.Map;

/**
 * Factory that creates ChatController with dependency injection.
 * Supports multiple AI backends through AiPortSelector.
 */
public class CompositionRoot {
    
    public static ChatControllerFactory createChatControllerFactory() {
        try {
            // Infrastructure adapters
            DatabaseManager dbManager = DatabaseManager.getInstance();

            UserRepository userRepository = new SqliteUserRepository();
            ChatRepository chatRepository = new SqliteChatRepository();
            MessageRepository messageRepository = new SqliteMessageRepository();
            
            // AI adapters
            AiPort pythonAiPort = new PythonAiClient();            // Local Qwen (port 5001)
            AiPort huggingFaceAiPort = new HuggingFaceAiClient();  // Spring HF (port 8080)
            
            // Model → AI port mapping
            Map<String, AiPort> aiPortMap = Map.of(
                "Qwen 1.5B Coder (CosmoChat Gateway)", pythonAiPort,
                "Qwen 7B Coder (HuggingFace API)", huggingFaceAiPort,
                "Mistral", huggingFaceAiPort,    // placeholder until dedicated Mistral client
                "Deepseek", huggingFaceAiPort    // placeholder until dedicated Deepseek client
            );
            
            AiPortSelector aiPortSelector = new AiPortSelector(aiPortMap);
            
            UsageTracking usageTracking = new UsageTrackingAdapter();
            SessionPort session = InMemorySessionService.getInstance();

            // Application use cases
            AuthenticateUser authenticateUser = new AuthenticateUserService(userRepository, new cosmochat.security.PasswordHasher());
            RegisterUser registerUser = new RegisterUserService(userRepository, new cosmochat.security.PasswordHasher());
            CreateChat createChat = new CreateChatService(chatRepository, userRepository);
            GetChatHistory getChatHistory = new GetChatHistoryService(chatRepository);
            GetChatMessages getChatMessages = new GetChatMessagesService(messageRepository);
            GetCurrentUser getCurrentUser = new GetCurrentUserService(session);
            DeleteChat deleteChat = new DeleteChatService(chatRepository, messageRepository, getCurrentUser);
            SendMessage sendMessage = new SendMessageService(
                messageRepository, chatRepository, userRepository, 
                aiPortSelector, usageTracking
            );
            LogoutUser logoutUser = new LogoutUserService(session);

            // Application service (orchestrator)
            ChatService chatService = new ChatService(
                authenticateUser, registerUser, createChat, getChatHistory,
                getChatMessages, sendMessage, getCurrentUser, logoutUser, 
                usageTracking, session, deleteChat
            );

            return new ChatControllerFactory(
                chatService,
                authenticateUser, registerUser, createChat,
                getChatHistory, getChatMessages, sendMessage,
                getCurrentUser, logoutUser, usageTracking
            );
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize infrastructure", e);
        }
    }

    /**
     * Factory for creating ChatController with all dependencies.
     * Public static nested class — acts like a top-level type.
     */
    public static class ChatControllerFactory {
        public final ChatService chatService;
        public final AuthenticateUser authenticateUser;
        public final RegisterUser registerUser;
        public final CreateChat createChat;
        public final GetChatHistory getChatHistory;
        public final GetChatMessages getChatMessages;
        public final SendMessage sendMessage;
        public final GetCurrentUser getCurrentUser;
        public final LogoutUser logoutUser;
        public final UsageTracking usageTracking;

        public ChatControllerFactory(
                ChatService chatService,
                AuthenticateUser authenticateUser,
                RegisterUser registerUser,
                CreateChat createChat,
                GetChatHistory getChatHistory,
                GetChatMessages getChatMessages,
                SendMessage sendMessage,
                GetCurrentUser getCurrentUser,
                LogoutUser logoutUser,
                UsageTracking usageTracking) {
            this.chatService = chatService;
            this.authenticateUser = authenticateUser;
            this.registerUser = registerUser;
            this.createChat = createChat;
            this.getChatHistory = getChatHistory;
            this.getChatMessages = getChatMessages;
            this.sendMessage = sendMessage;
            this.getCurrentUser = getCurrentUser;
            this.logoutUser = logoutUser;
            this.usageTracking = usageTracking;
        }

        public cosmochat.ChatController createChatController() {
            return new cosmochat.ChatController(chatService);
        }
    }
}


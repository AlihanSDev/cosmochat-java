package cosmochat;

import cosmochat.application.service.ChatService;
import cosmochat.application.dto.ChatDTO;
import cosmochat.application.dto.MessageDTO;
import cosmochat.application.dto.UsageStats;
import cosmochat.application.dto.SendMessageResult;
import cosmochat.application.dto.UserDTO;
import cosmochat.domain.Role;
import cosmochat.domain.UserId;
import cosmochat.ChatMessage;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import javafx.scene.control.Tooltip;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.concurrent.Task;

/**
 * ChatController (Presentation Layer) — thin UI controller.
 * Delegates all business logic to ChatService.
 */
public class ChatController extends StackPane {
    private static final double SIDEBAR_WIDTH = 280;
    private final ObservableList<ChatItem> chatHistory = FXCollections.observableArrayList();
    private ChatItem activeChat;
    private Integer activeChatId; // database ID
    private int nextChatId = 1;
    private boolean sidebarOpen = true;

    // Application Service (orchestrates use cases)
    private final ChatService chatService;

    // UI Components
    private VBox sidebar;
    private ListView<ChatItem> chatListView;
    private TextField inputField;
    private Button sendBtn;
    private VBox inputArea;
    private StackPane modalOverlay;
    private VBox toastContainer;
    private VBox sidebarFooter;
    private ScrollPane messagesScrollPane;
    private VBox messagesContainer;
    private StackPane chatViewContainer;
    private Label headerTitle;
    private VBox mainScreen;
    private VBox chatScreen;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
        initializeData();
        initializeUI();
        animateEntrance();
    }

    private void initializeData() {
        if (chatService.isLoggedIn()) {
            loadChatsFromDatabase();
        }
    }

    private void loadChatsFromDatabase() {
        List<ChatDTO> dbChats = chatService.getChatsForCurrentUser();
        for (ChatDTO chatDto : dbChats) {
            ChatItem item = new ChatItem(
                chatDto.id(),
                chatDto.title(),
                chatDto.date(),
                chatDto.iconGlyph()
            );
            // Load messages for this chat
            List<MessageDTO> msgDtos = chatService.getMessagesForChat(chatDto.id());
            for (MessageDTO msgDto : msgDtos) {
                ChatMessage.Role role = msgDto.role() == cosmochat.domain.Role.USER ? ChatMessage.Role.USER : ChatMessage.Role.AI;
                item.getMessages().add(new ChatMessage(role, msgDto.text(), msgDto.time()));
            }
            chatHistory.add(item);
        }
        if (!dbChats.isEmpty()) {
            nextChatId = dbChats.stream().mapToInt(ChatDTO::id).max().orElse(0) + 1;
        }
    }

    private void initializeUI() {
        HBox contentLayer = new HBox();
        sidebar = createSidebar();
        StackPane mainArea = createMainArea();
        HBox.setHgrow(mainArea, Priority.ALWAYS);

        Button menuToggle = new Button("☰");
        menuToggle.getStyleClass().add("menu-toggle");
        menuToggle.setOnAction(e -> toggleSidebar(true));
        StackPane.setAlignment(menuToggle, Pos.TOP_LEFT);
        StackPane.setMargin(menuToggle, new Insets(20, 0, 0, 20));
        menuToggle.setVisible(!sidebarOpen);

        toastContainer = new VBox(8);
        toastContainer.getStyleClass().add("toast-container");
        toastContainer.setPickOnBounds(false);
        StackPane.setAlignment(toastContainer, Pos.TOP_RIGHT);
        StackPane.setMargin(toastContainer, new Insets(24, 24, 0, 0));

        modalOverlay = new StackPane();
        modalOverlay.getStyleClass().add("modal-overlay");
        modalOverlay.setVisible(false);
        modalOverlay.setMouseTransparent(true);
        modalOverlay.setOnMouseClicked(e -> hideModal());

        contentLayer.getChildren().addAll(sidebar, mainArea);
        this.getChildren().addAll(contentLayer, menuToggle, toastContainer, modalOverlay);

        StackPane.setAlignment(contentLayer, Pos.TOP_LEFT);
        contentLayer.prefWidthProperty().bind(this.widthProperty());
        contentLayer.prefHeightProperty().bind(this.heightProperty());

        showMainScreen();
    }

    private StackPane createMainArea() {
        StackPane root = new StackPane();
        root.getStyleClass().add("main-area");

        VBox layout = new VBox();
        HBox header = new HBox();
        header.getStyleClass().add("chat-header");
        header.setAlignment(Pos.CENTER);
        headerTitle = new Label("CosmoChat");
        headerTitle.getStyleClass().add("chat-header-title");
        header.getChildren().add(headerTitle);

        chatViewContainer = new StackPane();
        chatViewContainer.getStyleClass().add("chat-view-container");
        VBox.setVgrow(chatViewContainer, Priority.ALWAYS);

        inputArea = createInputArea();

        mainScreen = new VBox();
        mainScreen.getStyleClass().add("main-screen");
        mainScreen.setAlignment(Pos.TOP_CENTER);
        mainScreen.setMaxWidth(680);
        mainScreen.setPadding(new Insets(24, 0, 0, 0));
        VBox logoSection = createLogoSection();
        Region divider = new Region();
        divider.getStyleClass().add("h-line");
        divider.setPrefHeight(1);
        HBox hints = createHints();
        mainScreen.getChildren().addAll(logoSection, inputArea, divider, hints);
        mainScreen.setVisible(true);

        chatScreen = new VBox();
        chatScreen.getStyleClass().add("chat-screen");
        chatScreen.setAlignment(Pos.CENTER);
        messagesScrollPane = new ScrollPane();
        messagesScrollPane.setFitToWidth(true);
        messagesScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        messagesScrollPane.getStyleClass().add("messages-scroll");
        messagesContainer = new VBox();
        messagesContainer.getStyleClass().add("messages-container");
        messagesContainer.setAlignment(Pos.TOP_LEFT);
        messagesScrollPane.setContent(messagesContainer);
        VBox.setVgrow(messagesScrollPane, Priority.ALWAYS);
        chatScreen.getChildren().add(messagesScrollPane);
        chatScreen.setVisible(false);

        chatViewContainer.getChildren().addAll(mainScreen, chatScreen);
        layout.getChildren().addAll(header, chatViewContainer);
        root.getChildren().add(layout);
        return root;
    }

    private VBox createInputArea() {
        VBox area = new VBox();
        area.getStyleClass().add("chat-input-area");
        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("chat-input-wrapper");
        inputField = new TextField();
        inputField.setPromptText("Спросите что угодно...");
        inputField.getStyleClass().add("chat-input-field");
        inputField.focusedProperty().addListener((obs, old, val) -> {
            if (val) wrapper.getStyleClass().add("input-wrapper-focused");
            else wrapper.getStyleClass().remove("input-wrapper-focused");
        });

        HBox bottomBar = new HBox();
        bottomBar.getStyleClass().add("input-bottom");
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        HBox actions = new HBox(4);
        Button attachBtn = new Button("📎");
        attachBtn.getStyleClass().add("action-btn");
        Button settingsBtn = new Button("⚙");
        settingsBtn.getStyleClass().add("action-btn");
        actions.getChildren().addAll(attachBtn, settingsBtn);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        sendBtn = new Button("↑");
        sendBtn.getStyleClass().add("send-btn");
        sendBtn.setDisable(true);
        inputField.textProperty().addListener((obs, old, val) -> {
            boolean hasText = !val.trim().isEmpty();
            sendBtn.setDisable(!hasText);
            if (hasText) sendBtn.getStyleClass().add("send-btn-active");
            else sendBtn.getStyleClass().remove("send-btn-active");
        });
        sendBtn.setOnAction(e -> sendMessage());
        inputField.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) sendMessage(); });

        bottomBar.getChildren().addAll(actions, spacer, sendBtn);
        wrapper.getChildren().addAll(inputField, bottomBar);
        area.getChildren().add(wrapper);
        return area;
    }

    private HBox createHints() {
        HBox hintsBox = new HBox(8);
        hintsBox.getStyleClass().add("hints");
        hintsBox.setAlignment(Pos.CENTER);
        String[][] hintData = {
            {"Чёрные дыры", "Расскажи о чёрных дырах"},
            {"Сколько звёзд?", "Сколько звёзд во Вселенной?"},
            {"За пределами", "Что за пределами Вселенной?"},
            {"Теория", "Объясни теорию относительности"}
        };
        for (String[] h : hintData) {
            Button chip = new Button(h[0]);
            chip.getStyleClass().add("hint-chip");
            chip.setOnAction(e -> {
                inputField.setText(h[1]);
                sendBtn.setDisable(false);
                sendBtn.getStyleClass().add("send-btn-active");
                inputField.requestFocus();
            });
            hintsBox.getChildren().add(chip);
        }
        return hintsBox;
    }

    private void showMainScreen() {
        if (mainScreen.isVisible()) return;
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), chatScreen);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            chatScreen.setVisible(false);
            chatScreen.setOpacity(1);
            chatScreen.getChildren().remove(inputArea);
            mainScreen.getChildren().add(1, inputArea);
            mainScreen.setOpacity(0);
            mainScreen.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), mainScreen);
            fadeIn.setToValue(1);
            fadeIn.play();
            headerTitle.setText("CosmoChat");
            chatListView.getSelectionModel().clearSelection();
            if (activeChat != null) {
                activeChat.setActive(false);
                activeChat = null;
                activeChatId = null;
            }
        });
        fadeOut.play();
    }

    private VBox createLogoSection() {
        VBox section = new VBox(16);
        section.setAlignment(Pos.CENTER);
        StackPane logoIconWrapper = new StackPane();
        logoIconWrapper.getStyleClass().add("logo-icon-wrapper");
        logoIconWrapper.setPrefSize(72, 72);
        Circle orbitDecor = new Circle(36, Color.TRANSPARENT);
        orbitDecor.setStroke(Color.rgb(255, 255, 255, 0.04));
        orbitDecor.setStrokeWidth(1);
        orbitDecor.setTranslateX(-12);
        orbitDecor.setTranslateY(-12);
        Circle mainOrbit = new Circle(18, Color.TRANSPARENT);
        mainOrbit.setStroke(Color.rgb(255, 255, 255, 0.12));
        mainOrbit.setStrokeWidth(0.8);
        mainOrbit.setRotate(-25);
        Circle center = new Circle(4, Color.web("white", 0.9));
        Circle innerRing = new Circle(6, Color.TRANSPARENT);
        innerRing.setStroke(Color.rgb(255, 255, 255, 0.15));
        innerRing.setStrokeWidth(0.5);
        logoIconWrapper.getChildren().addAll(orbitDecor, mainOrbit, innerRing, center);
        Label logoText = new Label("CosmoChat");
        logoText.getStyleClass().add("logo-text");
        Region line = new Region();
        line.setPrefSize(40, 1);
        line.setStyle("-fx-background-color: #3a3a44;");
        Label subtitle = new Label("Разговор с космосом");
        subtitle.getStyleClass().add("logo-subtitle");
        section.getChildren().addAll(logoIconWrapper, logoText, line, subtitle);
        return section;
    }

    private void createNewChat(String firstMessage) {
        String title = firstMessage.length() > 30 ? firstMessage.substring(0, 30) + "..." : firstMessage;
        String[] icons = {"★", "☄", "🚀", "🌍", "🛰", "🌙"};
        ChatItem newChat = new ChatItem(nextChatId++, title, "Сейчас", icons[(int)(Math.random() * icons.length)]);
        chatHistory.add(0, newChat);
        chatListView.getSelectionModel().select(0);
        switchToChat(newChat);
        // Mark as unsaved (new) chat — will be created on first message
        activeChatId = 0;
    }

    private void sendMessage() {
        if (!chatService.canSendMessage()) {
            showToast("Лимит сообщений исчерпан (100/час)");
            return;
        }

        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        // Если нет активного чата — создаём новый
        if (activeChat == null) {
            createNewChat(text);
        }

        // Добавляем сообщение пользователя
        addMessageLocally(ChatMessage.Role.USER, text);
        inputField.clear();
        sendBtn.setDisable(true);
        sendBtn.getStyleClass().remove("send-btn-active");

        scrollToBottom();
        showTypingIndicator();

        Task<SendMessageResult> aiTask = new Task<>() {
            @Override
            protected SendMessageResult call() throws Exception {
                int chatId = activeChatId != null ? activeChatId : 0;
                return chatService.sendMessage(chatId, text);
            }
        };

        aiTask.setOnSucceeded(e -> {
            removeTypingIndicator();
            SendMessageResult result = aiTask.getValue();
            addMessageLocally(ChatMessage.Role.AI, result.aiMessage().getText());
            // Если это был новый чат — обновляем ID
            if (activeChatId == 0) {
                activeChatId = result.chatId().getValue();
                if (activeChat != null) {
                    activeChat.setId(activeChatId);
                }
            }
            scrollToBottom();
        });

        aiTask.setOnFailed(e -> {
            removeTypingIndicator();
            Throwable ex = aiTask.getException();
            String errorMsg = ex.getMessage();
            if (errorMsg == null) errorMsg = ex.toString();
            String displayMsg = errorMsg.contains("not loaded") || errorMsg.contains("unavailable")
                ? "❌ Сервис ИИ недоступен. Запустите Python API сервер."
                : "❌ Ошибка: " + errorMsg;
            Platform.runLater(() -> addMessageLocally(ChatMessage.Role.AI, displayMsg));
            scrollToBottom();
        });

        new Thread(aiTask).start();
    }



     private void addMessageLocally(ChatMessage.Role role, String text) {
         ChatMessage msg = new ChatMessage(role, text, getTimeString());
         messagesContainer.getChildren().add(createMessageNode(msg));
         if (activeChat != null) {
             activeChat.getMessages().add(msg);
         }
     }

    private HBox createMessageNode(ChatMessage msg) {
        HBox messageBox = new HBox(10);
        messageBox.getStyleClass().addAll("message",
            msg.getRole() == ChatMessage.Role.USER ? "message--user" : "message--ai");

        Label avatar = new Label(msg.getRole() == ChatMessage.Role.USER ? "U" : "AI");
        avatar.getStyleClass().add("msg-avatar");

        VBox body = new VBox(4);
        Label bubble = new Label(msg.getText());
        bubble.getStyleClass().add("msg-bubble");
        bubble.setWrapText(true);

        Label time = new Label(msg.getTime());
        time.getStyleClass().add("msg-time");

        body.getChildren().addAll(bubble, time);

        if (msg.getRole() == ChatMessage.Role.USER) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            body.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getChildren().addAll(body, avatar);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
            body.setAlignment(Pos.CENTER_LEFT);
            messageBox.getChildren().addAll(avatar, body);
        }

        return messageBox;
    }

    private void showTypingIndicator() {
        HBox indicator = new HBox(10);
        indicator.getStyleClass().add("typing-indicator");
        indicator.setId("typingIndicator");
        Label avatar = new Label("AI");
        avatar.getStyleClass().add("msg-avatar");
        HBox dots = new HBox(5);
        dots.getStyleClass().add("typing-dots");
        dots.getChildren().addAll(new Circle(3, Color.GRAY), new Circle(3, Color.GRAY), new Circle(3, Color.GRAY));
        indicator.getChildren().addAll(avatar, dots);
        messagesContainer.getChildren().add(indicator);
    }

    private void removeTypingIndicator() {
        messagesContainer.getChildren().removeIf(n -> n.getId() != null && n.getId().equals("typingIndicator"));
    }

    private void scrollToBottom() {
        PauseTransition sc = new PauseTransition(Duration.millis(100));
        sc.setOnFinished(e -> messagesScrollPane.setVvalue(1.0));
        sc.play();
    }

    private String getTimeString() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    private VBox createSidebar() {
        VBox sidebarBox = new VBox();
        sidebarBox.getStyleClass().add("sidebar");
        sidebarBox.setPrefWidth(SIDEBAR_WIDTH);
        sidebarBox.setMinWidth(SIDEBAR_WIDTH);
        HBox header = new HBox(10);
        header.getStyleClass().add("sidebar-header");
        header.setAlignment(Pos.CENTER_LEFT);
        Label logoText = new Label("CosmoChat");
        logoText.getStyleClass().add("sidebar-logo-text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("sidebar-close");
        closeBtn.setOnAction(e -> toggleSidebar(false));
        header.getChildren().addAll(createLogoIcon(), logoText, spacer, closeBtn);

        Button newChatBtn = new Button("  Новый чат");
        newChatBtn.getStyleClass().add("new-chat-btn");
        newChatBtn.setOnAction(e -> {
            if (activeChat != null) activeChat.setActive(false);
            activeChat = null;
            activeChatId = null;
            showMainScreen(); // Показываем главный экран
            headerTitle.setText("CosmoChat");
            chatListView.getSelectionModel().clearSelection();
            showToast("Начните новый диалог");
        });

        VBox newChatWrapper = new VBox(newChatBtn);
        newChatWrapper.setPadding(new Insets(0, 16, 12, 16));

        TextField searchField = new TextField();
        searchField.setPromptText("Поиск чатов...");
        searchField.getStyleClass().add("sidebar-search");
        VBox searchWrapper = new VBox(searchField);
        searchWrapper.setPadding(new Insets(0, 16, 12, 16));

        Label historyLabel = new Label("ИСТОРИЯ");
        historyLabel.getStyleClass().add("sidebar-section-label");
        VBox.setMargin(historyLabel, new Insets(0, 16, 6, 16));

        chatListView = new ListView<>(chatHistory);
        chatListView.getStyleClass().add("chat-list");
        chatListView.setCellFactory(param -> new ChatListCell());
        VBox.setVgrow(chatListView, Priority.ALWAYS);
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) switchToChat(newVal);
        });

        sidebarFooter = new VBox(8);
        sidebarFooter.getStyleClass().add("sidebar-footer");
        updateFooter();

        sidebarBox.getChildren().addAll(header, newChatWrapper, searchWrapper, historyLabel, chatListView, sidebarFooter);
        return sidebarBox;
    }

    private void updateFooter() {
        sidebarFooter.getChildren().clear();
        if (chatService.isLoggedIn()) {
            HBox userBox = new HBox(8);
            userBox.setAlignment(Pos.CENTER_LEFT);

            UserDTO currentUser = chatService.getCurrentUser();
            Label userLabel = new Label(currentUser.username());
            userLabel.getStyleClass().add("auth-user-label");

            Button profileBtn = new Button("👤");
            profileBtn.getStyleClass().add("profile-btn");
            profileBtn.setTooltip(new Tooltip("Профиль"));
            profileBtn.setOnAction(e -> showProfileModal());

            userBox.getChildren().addAll(userLabel, profileBtn);

            Button logoutBtn = new Button("Выйти");
            logoutBtn.getStyleClass().addAll("auth-btn", "auth-btn--logout");
            logoutBtn.setOnAction(e -> logout());
            sidebarFooter.getChildren().addAll(userBox, logoutBtn);
        } else {
            Button loginBtn = new Button("Войти");
            loginBtn.getStyleClass().addAll("auth-btn", "auth-btn--login");
            loginBtn.setOnAction(e -> showModal("login"));
            Button registerBtn = new Button("Регистрация");
            registerBtn.getStyleClass().addAll("auth-btn", "auth-btn--register");
            registerBtn.setOnAction(e -> showModal("register"));
            sidebarFooter.getChildren().addAll(loginBtn, registerBtn);
        }
    }

    private void switchToChat(ChatItem chat) {
        if (activeChat == chat) return;

        if (activeChat != null) activeChat.setActive(false);
        activeChat = chat;
        activeChat.setActive(true);
        activeChatId = chat.getId(); // set active chat ID
        headerTitle.setText(chat.getTitle());
        messagesContainer.getChildren().clear();
        for (ChatMessage msg : chat.getMessages()) {
            messagesContainer.getChildren().add(createMessageNode(msg));
        }
        if (mainScreen.isVisible()) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), mainScreen);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                mainScreen.setVisible(false);
                mainScreen.setOpacity(1);
                mainScreen.getChildren().remove(inputArea);
                chatScreen.getChildren().add(inputArea);
                chatScreen.setOpacity(0);
                chatScreen.setVisible(true);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(200), chatScreen);
                fadeIn.setToValue(1);
                fadeIn.play();
                chatListView.refresh();
            });
            fadeOut.play();
        } else {
            if (!chatScreen.getChildren().contains(inputArea)) {
                chatScreen.getChildren().add(inputArea);
            }
            chatListView.refresh();
        }
    }

    private StackPane createLogoIcon() {
        StackPane icon = new StackPane();
        icon.setPrefSize(28, 28);
        Circle orbit = new Circle(10, Color.TRANSPARENT);
        orbit.setStroke(Color.rgb(255, 255, 255, 0.15));
        orbit.setStrokeWidth(0.7);
        orbit.setRotate(-25);
        Circle center = new Circle(2.5, Color.web("white", 0.85));
        Circle small = new Circle(1, Color.web("white", 0.5));
        small.setTranslateX(-8); small.setTranslateY(-4);
        icon.getChildren().addAll(orbit, center, small);
        return icon;
    }

    private void toggleSidebar(boolean open) {
        sidebarOpen = open;
        TranslateTransition trans = new TranslateTransition(Duration.millis(500), sidebar);
        trans.setInterpolator(Interpolator.SPLINE(0.22, 1, 0.36, 1));
        if (open) {
            trans.setToX(0);
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> this.lookup(".menu-toggle").setVisible(false));
            pause.play();
        } else {
            trans.setToX(-SIDEBAR_WIDTH - 20);
            this.lookup(".menu-toggle").setVisible(true);
        }
        trans.play();
    }

     private void showModal(String type) {
         VBox modalContent = new VBox(20);
         modalContent.getStyleClass().add("modal");
         modalContent.setMaxWidth(380);
         
         Button closeBtn = new Button("×");
         closeBtn.getStyleClass().add("modal-close");
         StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
         closeBtn.setOnAction(e -> hideModal());
         
         Label title = new Label(type.equals("login") ? "Вход" : "Регистрация");
         title.getStyleClass().add("modal-title");
         Label desc = new Label(type.equals("login") ? "Войдите, чтобы сохранять историю" : "Создайте аккаунт для синхронизации");
         desc.getStyleClass().add("modal-desc");
         
         VBox fields = new VBox(14);
         TextField nameField = new TextField();
         if (type.equals("register")) {
             Label nameLabel = new Label("ИМЯ");
             nameLabel.getStyleClass().add("modal-field-label");
             nameField.setPromptText("Ваше имя");
             nameField.getStyleClass().add("modal-field-input");
             fields.getChildren().addAll(nameLabel, nameField);
         }
         
         Label emailLabel = new Label("EMAIL");
         emailLabel.getStyleClass().add("modal-field-label");
         TextField emailField = new TextField();
         emailField.setPromptText("you@example.com");
         emailField.getStyleClass().add("modal-field-input");
         
         Label passwordLabel = new Label("ПАРОЛЬ");
         passwordLabel.getStyleClass().add("modal-field-label");
         TextField passwordField = new TextField();
         passwordField.setPromptText("Пароль");
         passwordField.getStyleClass().add("modal-field-input");
         
         fields.getChildren().addAll(emailLabel, emailField, passwordLabel, passwordField);
         
         Button submitBtn = new Button(type.equals("login") ? "Войти" : "Создать аккаунт");
         submitBtn.getStyleClass().add("modal-submit");
         
         Label errorLabel = new Label();
         errorLabel.setStyle("-fx-text-fill: #ff6b6b; -fx-font-size: 12px;");
         errorLabel.setVisible(false);
         
         submitBtn.setOnAction(e -> {
             String email = emailField.getText().trim();
             String password = passwordField.getText();
             if (email.isEmpty() || password.isEmpty()) {
                 errorLabel.setText("Заполните email и пароль");
                 errorLabel.setVisible(true);
                 return;
             }
             submitBtn.setDisable(true);
             Task<Boolean> authTask = new Task<>() {
                 @Override
                 protected Boolean call() throws Exception {
                     if (type.equals("login")) {
                         return chatService.login(email, password);
                     } else {
                         String username = nameField.getText().trim();
                         if (username.isEmpty()) {
                             throw new Exception("Введите имя");
                         }
                         return chatService.register(username, email, password);
                     }
                 }
             };
             authTask.setOnSucceeded(ev -> {
                 submitBtn.setDisable(false);
                 boolean success = authTask.getValue();
                 if (success) {
                     hideModal();
                     loadChatsFromDatabase();
                     updateFooter();
                     showToast(type.equals("login") ? "Добро пожаловать!" : "Аккаунт создан");
                 } else {
                     errorLabel.setText(type.equals("login") ? "Неверный email или пароль" : "Email уже занят");
                     errorLabel.setVisible(true);
                 }
             });
             authTask.setOnFailed(ev -> {
                 submitBtn.setDisable(false);
                 Throwable ex = authTask.getException();
                 errorLabel.setText("Ошибка: " + ex.getMessage());
                 errorLabel.setVisible(true);
             });
             new Thread(authTask).start();
         });
         
         StackPane wrapper = new StackPane();
         wrapper.getChildren().addAll(modalContent, closeBtn);
         modalContent.getChildren().addAll(title, desc, fields, errorLabel, submitBtn);
         modalOverlay.getChildren().setAll(wrapper);
         modalOverlay.setVisible(true);
         modalOverlay.setMouseTransparent(false);
     }
 
     private VBox createField(String label, String prompt) {
         VBox box = new VBox(6);
         Label lbl = new Label(label);
         lbl.getStyleClass().add("modal-field-label");
         TextField field = new TextField();
         field.setPromptText(prompt);
         field.getStyleClass().add("modal-field-input");
         box.getChildren().addAll(lbl, field);
         return box;
      }
  
      private void hideModal() {
          modalOverlay.setVisible(false);
          modalOverlay.setMouseTransparent(true);
      }
  
     private void showProfileModal() {
         if (!chatService.isLoggedIn()) return;

         VBox modalContent = new VBox(16);
         modalContent.getStyleClass().add("modal");
         modalContent.setMaxWidth(400);
         modalContent.setPrefWidth(380);

         Button closeBtn = new Button("×");
         closeBtn.getStyleClass().add("modal-close");
         StackPane.setAlignment(closeBtn, Pos.TOP_RIGHT);
         closeBtn.setOnAction(e -> hideModal());

         // Header with avatar
         HBox header = new HBox(16);
         header.setAlignment(Pos.CENTER_LEFT);

         StackPane avatarWrapper = new StackPane();
         avatarWrapper.getStyleClass().add("profile-avatar-wrapper");
         UserDTO currentUser = chatService.getCurrentUser();
         Label avatar = new Label(currentUser.username().substring(0, 1).toUpperCase());
         avatar.getStyleClass().add("profile-avatar");
         avatarWrapper.getChildren().add(avatar);

         VBox titleBox = new VBox(4);
         Label nameLabel = new Label(currentUser.username());
         nameLabel.getStyleClass().add("profile-name");
         Label emailLabel = new Label(currentUser.email());
         emailLabel.getStyleClass().add("profile-email");
         titleBox.getChildren().addAll(nameLabel, emailLabel);

         header.getChildren().addAll(avatarWrapper, titleBox);
         HBox.setHgrow(titleBox, Priority.ALWAYS);

         // Divider
         Region divider = new Region();
         divider.getStyleClass().add("profile-divider");

         // Usage stats
         VBox statsBox = new VBox(12);
         statsBox.getStyleClass().add("profile-stats");

         UsageStats stats = chatService.getUsageStats();

         // Messages sent label
         Label sentLabel = new Label("Отправлено");
         sentLabel.getStyleClass().add("profile-stat-label");
         Label sentCount = new Label(stats != null ? String.valueOf(stats.messagesSent()) : "—");
         sentCount.getStyleClass().add("profile-stat-value");

         HBox sentBox = new HBox();
         sentBox.setAlignment(Pos.CENTER_LEFT);
         Region sentSpacer = new Region();
         HBox.setHgrow(sentSpacer, Priority.ALWAYS);
         sentBox.getChildren().addAll(sentLabel, sentSpacer, sentCount);

         // Limit
         Label limitLabel = new Label("Лимит в час");
         limitLabel.getStyleClass().add("profile-stat-label");
         Label limitValue = new Label("100");
         limitValue.getStyleClass().add("profile-stat-value");
         HBox limitBox = new HBox();
         limitBox.setAlignment(Pos.CENTER_LEFT);
         Region limitSpacer = new Region();
         HBox.setHgrow(limitSpacer, Priority.ALWAYS);
         limitBox.getChildren().addAll(limitLabel, limitSpacer, limitValue);

         // Progress bar
         ProgressBar progress = new ProgressBar(stats != null ? stats.getUsagePercentage() / 100.0 : 0);
         progress.getStyleClass().add("profile-progress");
         progress.setPrefWidth(260);

         // Remaining time
         Label timeLabel = new Label();
         if (stats != null && !stats.isWindowExpired()) {
             timeLabel.setText("Сброс лимита через: " + formatTimeRemaining(stats.windowEnd()));
             timeLabel.getStyleClass().add("profile-time-label");
         } else {
             timeLabel.setText("Лимит сброшен");
             timeLabel.getStyleClass().add("profile-time-label-reset");
         }

         statsBox.getChildren().addAll(sentBox, limitBox, progress, timeLabel);

         StackPane wrapper = new StackPane();
         wrapper.getChildren().addAll(modalContent, closeBtn);
         modalContent.getChildren().addAll(header, divider, statsBox);
         modalOverlay.getChildren().setAll(wrapper);
         modalOverlay.setVisible(true);
         modalOverlay.setMouseTransparent(false);

         // Auto-refresh time every minute
         final UsageStats finalStats = stats;
         Timeline refresh = new Timeline(new KeyFrame(Duration.minutes(1), e -> {
             if (finalStats != null && !finalStats.isWindowExpired()) {
                 timeLabel.setText("Сброс лимита через: " + formatTimeRemaining(finalStats.windowEnd()));
             }
         }));
         refresh.setCycleCount(Animation.INDEFINITE);
          refresh.play();
      }
  
      private String formatTimeRemaining(java.time.LocalDateTime windowEnd) {
        java.time.Duration d = java.time.Duration.between(java.time.LocalDateTime.now(), windowEnd);
        long hours = d.toHours();
        long minutes = d.toMinutes() % 60;
        long seconds = d.getSeconds() % 60;
        if (hours > 0) return String.format("%d ч %d мин", hours, minutes);
        if (minutes > 0) return String.format("%d мин %d сек", minutes, seconds);
        return String.format("%d сек", seconds);
     }

     private void showToast(String message) {
        HBox toast = new HBox(10);
        toast.getStyleClass().add("toast");
        toast.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("✓");
        icon.getStyleClass().add("toast-icon");
        Label text = new Label(message);
        toast.getChildren().addAll(icon, text);
        toastContainer.getChildren().add(toast);
        toast.setTranslateX(200);
        toast.setOpacity(0);
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(toast.translateXProperty(), 200), new KeyValue(toast.opacityProperty(), 0)),
            new KeyFrame(Duration.millis(400), new KeyValue(toast.translateXProperty(), 0), new KeyValue(toast.opacityProperty(), 1)),
            new KeyFrame(Duration.seconds(3), new KeyValue(toast.opacityProperty(), 1)),
            new KeyFrame(Duration.millis(400), new KeyValue(toast.translateXProperty(), 200), new KeyValue(toast.opacityProperty(), 0))
        );
        timeline.setOnFinished(e -> toastContainer.getChildren().remove(toast));
        timeline.play();
    }

    private void animateEntrance() {
        this.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(1200), this);
        ft.setFromValue(0); ft.setToValue(1); ft.setDelay(Duration.millis(300));
        ft.play();
    }

    private class ChatListCell extends ListCell<ChatItem> {
        private final HBox root;
        private final Label iconLabel;
        private final Label titleLabel;
        private final Label dateLabel;
        private final HBox actionsBox;

        public ChatListCell() {
            root = new HBox(10);
            root.getStyleClass().add("chat-item");
            root.setAlignment(Pos.CENTER_LEFT);
            StackPane iconWrap = new StackPane();
            iconWrap.getStyleClass().add("chat-item-icon");
            iconWrap.setPrefSize(32, 32);
            iconLabel = new Label();
            iconWrap.getChildren().add(iconLabel);
            VBox info = new VBox(2);
            info.getStyleClass().add("chat-item-info");
            titleLabel = new Label();
            titleLabel.getStyleClass().add("chat-item-title");
            dateLabel = new Label();
            dateLabel.getStyleClass().add("chat-item-date");
            info.getChildren().addAll(titleLabel, dateLabel);
            HBox.setHgrow(info, Priority.ALWAYS);
            actionsBox = new HBox(2);
            actionsBox.getStyleClass().add("chat-item-actions");
            actionsBox.setVisible(false);
            Button deleteBtn = new Button("🗑");
            deleteBtn.getStyleClass().add("chat-item-action");
            deleteBtn.setStyle("-fx-text-fill: #e55;");
            deleteBtn.setOnAction(e -> {
                // Delete via service? For now UI-only
                chatHistory.remove(getItem());
                if (activeChat == getItem()) {
                    activeChat = null;
                    activeChatId = null;
                    showMainScreen();
                }
                showToast("Чат удален");
            });
            actionsBox.getChildren().addAll(deleteBtn);
            root.getChildren().addAll(iconWrap, info, actionsBox);
            root.hoverProperty().addListener((obs, old, val) -> actionsBox.setVisible(val));
        }

        @Override
        protected void updateItem(ChatItem item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setGraphic(null);
            else {
                iconLabel.setText(item.getIconGlyph());
                titleLabel.setText(item.getTitle());
                dateLabel.setText(item.getDate());
                if (item.isActive()) root.getStyleClass().add("chat-item-active");
                else root.getStyleClass().remove("chat-item-active");
                setGraphic(root);
            }
        }
    }

    private void logout() {
        chatService.logout();
        chatHistory.clear();
        if (activeChat != null) {
            activeChat.setActive(false);
            activeChat = null;
            activeChatId = null;
        }
        showMainScreen();
        updateFooter();
        showToast("Вы вышли из аккаунта");
    }
}

package cosmochat;

import cosmochat.application.service.ChatService;
import cosmochat.application.dto.ChatDTO;
import cosmochat.application.dto.MessageDTO;
import cosmochat.application.dto.UsageStats;
import cosmochat.application.dto.SendMessageResult;
import cosmochat.application.dto.SendMessageCommand;
import cosmochat.application.dto.UserDTO;
import cosmochat.domain.Role;
import cosmochat.domain.UserId;
import cosmochat.domain.ChatId;
import cosmochat.ChatMessage;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
 import javafx.scene.control.*;
 import javafx.scene.input.KeyCode;
 import javafx.scene.input.Clipboard;
 import javafx.scene.input.ClipboardContent;
 import javafx.scene.layout.*;
 import javafx.scene.paint.Color;
 import javafx.scene.shape.Circle;
 import javafx.scene.web.WebEngine;
 import javafx.scene.web.WebView;
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
    private ComboBox<String> modelSelector;
    private StackPane modalOverlay;
    private VBox toastContainer;
    private VBox sidebarFooter;
    private ScrollPane messagesScrollPane;
    private VBox messagesContainer;
    private StackPane chatViewContainer;
    private Label headerTitle;
     private VBox mainScreen;
     private VBox chatScreen;
      private String selectedModel = "Qwen 1.5B Coder (CosmoChat Gateway)";
      private Button testHtmlBtn;
     private Timeline profileRefreshTimeline;
     // Profile UI components for live updates
     private Label profileUsedValue;
     private Label profileRemainingValue;
     private ProgressBar profileUsageProgress;
     private Label profileWindowInfo;
     private UsageStats profileStats;

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
        HBox actions = new HBox(6);
        Button attachBtn = new Button("📎");
        attachBtn.getStyleClass().add("action-btn");

        // Model selector
        modelSelector = new ComboBox<>();
        modelSelector.getItems().addAll(
            "Qwen 1.5B Coder (CosmoChat Gateway)",
            "Qwen 7B Coder (HuggingFace API)",
            "Mistral",
            "Deepseek"
        );
        modelSelector.getSelectionModel().selectFirst();
        modelSelector.getStyleClass().add("model-selector");
        modelSelector.setTooltip(new Tooltip("Выберите модель ИИ"));
        modelSelector.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                selectedModel = newVal;
                showToast("Модель изменена: " + newVal.split(" \\(")[0]);
            }
        });

        Button settingsBtn = new Button("⚙");
        settingsBtn.getStyleClass().add("action-btn");
        actions.getChildren().addAll(attachBtn, modelSelector, settingsBtn);
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
         
         // Test HTML button
         testHtmlBtn = new Button("📄 Test HTML");
         testHtmlBtn.getStyleClass().add("hint-chip");
         testHtmlBtn.setTooltip(new Tooltip("Отправить тестовый HTML в чат"));
         testHtmlBtn.setOnAction(e -> sendTestHtmlMessage());
         hintsBox.getChildren().add(testHtmlBtn);
         
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
                UserDTO currentUser = chatService.getCurrentUser();
                SendMessageCommand cmd = new SendMessageCommand(
                    new UserId(currentUser.id()),
                    chatId == 0 ? null : new ChatId(chatId),
                    text,
                    selectedModel
                );
                return chatService.sendMessage(cmd);
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
            final String finalErrorMsg = errorMsg;
            Platform.runLater(() -> addMessageLocally(ChatMessage.Role.AI, finalErrorMsg));
            scrollToBottom();
        });

        new Thread(aiTask).start();
    }



      private void addMessageLocally(ChatMessage.Role role, String text) {
          // Store full original text; detect if AI message contains HTML block
          boolean containsHtml = (role == ChatMessage.Role.AI) && containsHtmlBlock(text);
          ChatMessage msg = new ChatMessage(role, text, getTimeString());
          messagesContainer.getChildren().add(createMessageNode(msg, containsHtml));
          if (activeChat != null) {
              activeChat.getMessages().add(msg);
          }
      }

     /**
      * Heuristically checks if a string looks like HTML content.
      */
      private boolean looksLikeHtml(String s) {
          String lower = s.toLowerCase();
          return (s.startsWith("<!DOCTYPE") || s.startsWith("<html") || 
                  lower.contains("<head") && lower.contains("<body") ||
                  (lower.contains("<!doctype") && lower.contains("</html>")));
      }
      
      /**
       * Checks if the AI message contains an HTML code block (```html ... ``` or raw HTML)
       */
      private boolean containsHtmlBlock(String text) {
          if (text == null) return false;
          
          // Check for markdown code block with html
          if (text.contains("```html") || text.contains("```\n<!DOCTYPE") || text.contains("```\n<html")) {
              return true;
          }
          // Check for ``` ... ``` that contains HTML
          if (text.contains("```")) {
              int firstFence = text.indexOf("```");
              int secondFence = text.indexOf("```", firstFence + 3);
              if (secondFence != -1) {
                  String between = text.substring(firstFence + 3, secondFence).trim();
                  if (looksLikeHtml(between)) return true;
              }
          }
          // Check for raw HTML without fences (if message starts with HTML)
          if (text.trim().startsWith("<!DOCTYPE") || text.trim().startsWith("<html")) {
              return true;
          }
          return false;
      }

     private void addMessageLocally(ChatMessage.Role role, String text, boolean isHtml) {
         ChatMessage msg = new ChatMessage(role, text, getTimeString());
         messagesContainer.getChildren().add(createMessageNode(msg, isHtml));
         if (activeChat != null) {
             activeChat.getMessages().add(msg);
         }
     }

     private HBox createMessageNode(ChatMessage msg) {
         return createMessageNode(msg, false);
     }
     
     private HBox createMessageNode(ChatMessage msg, boolean isHtml) {
         HBox messageBox = new HBox(10);
         messageBox.getStyleClass().addAll("message",
             msg.getRole() == ChatMessage.Role.USER ? "message--user" : "message--ai");
     
         Label avatar = new Label(msg.getRole() == ChatMessage.Role.USER ? "U" : "AI");
         avatar.getStyleClass().add("msg-avatar");
     
         VBox body = new VBox(4);
         
          if (isHtml) {
              // Split message into: before HTML, HTML block, after HTML
              MessageParts parts = splitMessageIntoParts(msg.getText());
              
              // Before text (intro)
              if (parts.before != null && !parts.before.isBlank()) {
                  Label beforeLabel = new Label(parts.before);
                  beforeLabel.getStyleClass().add("msg-bubble");
                  beforeLabel.setWrapText(true);
                  body.getChildren().add(beforeLabel);
              }
              
              // HTML WebView
              WebView webView = new WebView();
              webView.getStyleClass().add("msg-html");
              WebEngine engine = webView.getEngine();
              engine.setJavaScriptEnabled(true);
              engine.loadContent(parts.html);
              webView.setPrefHeight(400);
              webView.setMinHeight(300);
              webView.setMaxHeight(500);
              body.getChildren().add(webView);
              
              // Copy button for HTML (copies HTML code)
              Label copyBtn = new Label("📋 Копировать HTML");
              copyBtn.getStyleClass().add("msg-copy-btn");
              copyBtn.setTooltip(new Tooltip("Копировать HTML код"));
              copyBtn.setOnMouseClicked(e -> {
                  Clipboard clipboard = Clipboard.getSystemClipboard();
                  ClipboardContent content = new ClipboardContent();
                  content.putString(parts.html);
                  clipboard.setContent(content);
                  showToast("HTML скопирован");
              });
              HBox copyBox = new HBox(copyBtn);
              copyBox.setAlignment(Pos.CENTER_RIGHT);
              body.getChildren().add(copyBox);
              
              // After text (outro)
              if (parts.after != null && !parts.after.isBlank()) {
                  Label afterLabel = new Label(parts.after);
                  afterLabel.getStyleClass().add("msg-bubble");
                  afterLabel.setWrapText(true);
                  body.getChildren().add(afterLabel);
              }
          } else {
             Label bubble = new Label(msg.getText());
             bubble.getStyleClass().add("msg-bubble");
             bubble.setWrapText(true);
             body.getChildren().add(bubble);
             
             // Copy button (appears below bubble, aligned right)
             Label copyBtn = new Label("📋 Копировать");
             copyBtn.getStyleClass().add("msg-copy-btn");
             copyBtn.setTooltip(new Tooltip("Копировать сообщение"));
             copyBtn.setOnMouseClicked(e -> {
                 javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                 javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                 content.putString(msg.getText());
                 clipboard.setContent(content);
                 showToast("Скопировано");
             });
             HBox copyBox = new HBox(copyBtn);
             copyBox.setAlignment(Pos.CENTER_RIGHT);
             body.getChildren().add(copyBox);
         }
         
         Label time = new Label(msg.getTime());
         time.getStyleClass().add("msg-time");
         body.getChildren().add(time);
     
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
      
      /**
       * Splits an AI message that contains HTML code block into three parts:
       * text before the code, the HTML code itself, and text after.
       */
      private MessageParts splitMessageIntoParts(String fullText) {
          String before = null, html = null, after = null;
          
          // Try to find ```html ... ``` block first
          String text = fullText;
          int startIdx = -1;
          int endIdx = -1;
          
          // Search for ```html
          startIdx = text.indexOf("```html");
          if (startIdx != -1) {
              endIdx = text.indexOf("```", startIdx + 7);
              if (endIdx != -1) {
                  before = text.substring(0, startIdx).trim();
                  html = text.substring(startIdx + 7, endIdx).trim();
                  after = text.substring(endIdx + 3).trim();
                  return new MessageParts(before, html, after);
              }
          }
          
          // Fallback: any ```...``` block
          startIdx = text.indexOf("```");
          if (startIdx != -1) {
              endIdx = text.indexOf("```", startIdx + 3);
              if (endIdx != -1) {
                  before = text.substring(0, startIdx).trim();
                  html = text.substring(startIdx + 3, endIdx).trim();
                  after = text.substring(endIdx + 3).trim();
                  if (looksLikeHtml(html)) {
                      return new MessageParts(before, html, after);
                  }
              }
          }
          
          // No code block found — treat whole text as HTML (raw)
          if (looksLikeHtml(text.trim())) {
              return new MessageParts(null, text.trim(), null);
          }
          
          // Fallback: return as plain text parts (no HTML)
          return new MessageParts(null, text, null);
      }
      
      /**
       * Simple data holder for the three parts of a message with HTML.
       */
      private static class MessageParts {
          final String before;
          final String html;
          final String after;
          MessageParts(String before, String html, String after) {
              this.before = before;
              this.html = html;
              this.after = after;
          }
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
             boolean isHtml = (msg.getRole() == ChatMessage.Role.AI) && containsHtmlBlock(msg.getText());
             messagesContainer.getChildren().add(createMessageNode(msg, isHtml));
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
          // Stop profile timer if running (profile modal is being replaced)
          if (profileRefreshTimeline != null) {
              profileRefreshTimeline.stop();
              profileRefreshTimeline = null;
          }
          // Clear profile component references
          profileUsedValue = null;
          profileRemainingValue = null;
          profileUsageProgress = null;
          profileWindowInfo = null;
          profileStats = null;

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

          // Start live timer to update remaining time every second
          startProfileTimer();
      }

      private void startProfileTimer() {
          // Stop any existing timer
          if (profileRefreshTimeline != null) {
              profileRefreshTimeline.stop();
          }

          // Create new timer that updates every second
          profileRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
              if (profileStats != null) {
                  if (!profileStats.isWindowExpired()) {
                      profileWindowInfo.setText("Окно лимитов сбрасывается через: " + formatTimeRemaining(profileStats.windowEnd()));
                  } else {
                      profileWindowInfo.setText("Лимиты сброшены — окно обновлено");
                  }
              }
          }));
          profileRefreshTimeline.setCycleCount(Animation.INDEFINITE);
          profileRefreshTimeline.play();
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
             // Stop profile timer if running
             if (profileRefreshTimeline != null) {
                 profileRefreshTimeline.stop();
                 profileRefreshTimeline = null;
             }
             // Clear profile component references
             profileUsedValue = null;
             profileRemainingValue = null;
             profileUsageProgress = null;
             profileWindowInfo = null;
             profileStats = null;

             modalOverlay.setVisible(false);
             modalOverlay.setMouseTransparent(true);
          }

        private void sendTestHtmlMessage() {
           if (!chatService.canSendMessage()) {
               showToast("Лимит сообщений исчерпан (100/час)");
               return;
           }
           
           String htmlContent = "<!DOCTYPE html>\n" +
               "<html lang=\"ru\">\n" +
               "<head>\n" +
               "<meta charset=\"utf-8\"/>\n" +
               "<meta content=\"width=device-width, initial-scale=1.0\" name=\"viewport\"/>\n" +
               "<title>Spring Framework</title>\n" +
               "<script src=\"https://cdn.tailwindcss.com\"></script>\n" +
               "<link href=\"https://fonts.googleapis.com/css2?family=Roboto:wght@300;400;500;700;900&family=Roboto+Mono:wght@400;500;600&display=swap\" rel=\"stylesheet\"/>\n" +
               "<link href=\"https://cdn.cn.font.mi.com/font/css?family=MiSans:300,400,500,600,700:Chinese_Simplify,Latin&display=swap\" rel=\"stylesheet\"/>\n" +
               "<link href=\"https://fonts.googleapis.com/icon?family=Material+Icons\" rel=\"stylesheet\"/>\n" +
               "<style type=\"text/tailwindcss\">\n" +
               "@layer utilities {\n" +
               ".ppt-slide {\n" +
               "@apply relative w-[1280px] h-[720px] mx-auto box-border\n" +
               "}\n" +
               "}\n" +
               "</style>\n" +
               "<style>body {\n" +
               "    font-family: \"MiSans\", \"Roboto\", sans-serif;\n" +
               "    background: #f5f5f5\n" +
               "    }\n" +
               ".title-font {\n" +
               "    font-family: \"MiSans\", \"Roboto\", sans-serif\n" +
               "    }\n" +
               ".code-font {\n" +
               "    font-family: \"Roboto Mono\", monospace\n" +
               "    }</style>\n" +
               "<style>.material-icons {\n" +
               "    width: 1em\n" +
               "    }</style><style>.material-icons {\n" +
               "    direction: rtl\n" +
               "    }</style></head>\n" +
               "<body>\n" +
               "<div class=\"ppt-slide flex justify-center items-center bg-white\" style=\"height: 720px\">\n" +
               "<div class=\"absolute top-8 right-8 opacity-20\">\n" +
               "<img alt=\"Spring Logo\" class=\"w-24 h-24 object-contain\" src=\"https://sfile.chatglm.cn/images-ppt/c4e49bf4ea9d.png\"/>\n" +
               "</div>\n" +
               "<div class=\"relative z-10 text-center px-20\">\n" +
               "<div class=\"mb-6\">\n" +
               "<div class=\"w-20 h-20 mx-auto mb-8 flex items-center justify-center\">\n" +
               "<img alt=\"Spring Logo\" class=\"w-full h-full object-contain\" src=\"https://sfile.chatglm.cn/images-ppt/c4e49bf4ea9d.png\"/>\n" +
               "</div>\n" +
               "</div>\n" +
               "<h1 class=\"title-font font-bold text-blue-700 mb-8 leading-tight\" style=\"font-size: 64px\">\n" +
               "Spring Framework\n" +
               "</h1>\n" +
               "<div class=\"flex items-center justify-center gap-4 mb-8\">\n" +
               "<div class=\"h-0.5 w-16 bg-blue-300\"></div>\n" +
               "<div class=\"w-2 h-2 rounded-full bg-blue-400\"></div>\n" +
               "<div class=\"h-0.5 w-16 bg-blue-300\"></div>\n" +
               "</div>\n" +
               "<p class=\"title-font text-2xl font-medium text-gray-700 tracking-wide\">\n" +
               "Мощный фреймворк для разработки Java-приложений\n" +
               "</p>\n" +
               "<div class=\"mt-12 flex justify-center gap-8 text-sm text-gray-500\">\n" +
               "<div class=\"flex items-center gap-2\">\n" +
               "<i class=\"material-icons text-blue-500\" style=\"font-size: 20px\">code</i>\n" +
               "<span>IoC & DI</span>\n" +
               "</div>\n" +
               "<div class=\"flex items-center gap-2\">\n" +
               "<i class=\"material-icons text-blue-500\" style=\"font-size: 20px\">web</i>\n" +
               "<span>Spring MVC</span>\n" +
               "</div>\n" +
               "<div class=\"flex items-center gap-2\">\n" +
               "<i class=\"material-icons text-blue-500\" style=\"font-size: 20px\">storage</i>\n" +
               "<span>Spring Data</span>\n" +
               "</div>\n" +
               "<div class=\"flex items-center gap-2\">\n" +
               "<i class=\"material-icons text-blue-500\" style=\"font-size: 20px\">rocket_launch</i>\n" +
               "<span>Spring Boot</span>\n" +
               "</div>\n" +
               "</div>\n" +
               "</div>\n" +
               "</div>\n" +
               "</body>\n" +
               "</html>";
           
           if (activeChat == null) {
               createNewChat(htmlContent);
           }
           
           addMessageLocally(ChatMessage.Role.AI, htmlContent, true);
           scrollToBottom();
           showToast("HTML тест отправлен");
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

          // Usage stats - styled like AI services (OpenAI/Anthropic)
          UsageStats stats = chatService.getUsageStats();
          profileStats = stats; // store for timer updates

          VBox usageBox = new VBox(10);
          usageBox.getStyleClass().add("profile-usage-box");

          // Header
          Label usageHeader = new Label("Использование модели");
          usageHeader.getStyleClass().add("profile-usage-header");

          // Current usage metrics
          HBox metricsRow = new HBox(20);
          metricsRow.setAlignment(Pos.CENTER);

          // Used messages
          VBox usedBox = new VBox(4);
          usedBox.setAlignment(Pos.CENTER);
          profileUsedValue = new Label(stats != null ? String.valueOf(stats.messagesSent()) : "0");
          profileUsedValue.getStyleClass().add("profile-usage-value");
          Label usedLabel = new Label("Отправлено");
          usedLabel.getStyleClass().add("profile-usage-label");
          usedBox.getChildren().addAll(profileUsedValue, usedLabel);

          // Separator
          Region midSeparator = new Region();
          midSeparator.setPrefWidth(1);
          midSeparator.setStyle("-fx-background-color: rgba(255,255,255,0.15);");

          // Remaining messages
          VBox remainingBox = new VBox(4);
          remainingBox.setAlignment(Pos.CENTER);
          int remaining = stats != null ? stats.hourlyLimit() - stats.messagesSent() : 100;
          profileRemainingValue = new Label(String.valueOf(remaining));
          profileRemainingValue.getStyleClass().add("profile-usage-value-highlight");
          Label remainingLabel = new Label("До сброса");
          remainingLabel.getStyleClass().add("profile-usage-label");
          remainingBox.getChildren().addAll(profileRemainingValue, remainingLabel);

          metricsRow.getChildren().addAll(usedBox, midSeparator, remainingBox);

          // Progress bar with window info
          profileUsageProgress = new ProgressBar(stats != null ? stats.getUsagePercentage() / 100.0 : 0);
          profileUsageProgress.getStyleClass().add("profile-usage-progress");
          profileUsageProgress.setPrefWidth(300);

          // Window reset info
          profileWindowInfo = new Label();
          if (stats != null && !stats.isWindowExpired()) {
              profileWindowInfo.setText("Окно лимитов сбрасывается через: " + formatTimeRemaining(stats.windowEnd()));
          } else {
              profileWindowInfo.setText("Лимиты сброшены — окно обновлено");
          }
          profileWindowInfo.getStyleClass().add("profile-window-info");

          usageBox.getChildren().addAll(usageHeader, metricsRow, profileUsageProgress, profileWindowInfo);

          // Container for usage stats
          VBox statsContainer = new VBox(12);
          statsContainer.getStyleClass().add("profile-stats");
          statsContainer.getChildren().add(usageBox);

          modalContent.getChildren().addAll(header, divider, statsContainer);

          StackPane wrapper = new StackPane();
          wrapper.getChildren().addAll(modalContent, closeBtn);
          modalOverlay.getChildren().setAll(wrapper);
          modalOverlay.setVisible(true);
          modalOverlay.setMouseTransparent(false);

          // Start live timer to update remaining time every second
          startProfileTimer();
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
                ChatItem item = getItem();
                if (item == null) return;
                int chatId = item.getId();
                try {
                    boolean deleted = chatService.deleteChat(chatId);
                    if (deleted) {
                        chatHistory.remove(item);
                        if (activeChat == item) {
                            activeChat = null;
                            activeChatId = null;
                            showMainScreen();
                        }
                        showToast("Чат удален");
                    } else {
                        showToast("Не удалось удалить чат");
                    }
                } catch (Exception ex) {
                    showToast("Ошибка удаления: " + ex.getMessage());
                }
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

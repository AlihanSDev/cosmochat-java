package cosmochat;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import cosmochat.infrastructure.CompositionRoot;

public class CosmoChatApp extends Application {
    private double xOffset = 0, yOffset = 0;
    private double prevX, prevY, prevW, prevH;
    private boolean isCustomMaximized = false;

    @Override
    public void start(Stage primaryStage) {
        VBox root = new VBox();
        root.getStyleClass().add("root-container");

        HBox titleBar = createTitleBar(primaryStage);
        StarfieldCanvas starfield = new StarfieldCanvas();

        // Use Composition Root to create ChatController with DI
        CompositionRoot.ChatControllerFactory factory = CompositionRoot.createChatControllerFactory();
        ChatController controller = factory.createChatController();

        StackPane contentStack = new StackPane();
        starfield.widthProperty().bind(contentStack.widthProperty());
        starfield.heightProperty().bind(contentStack.heightProperty());
        contentStack.getChildren().addAll(starfield, controller);

        VBox.setVgrow(contentStack, Priority.ALWAYS);
        root.getChildren().addAll(titleBar, contentStack);

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        scene.setFill(Color.TRANSPARENT);

        primaryStage.initStyle(StageStyle.TRANSPARENT);
        primaryStage.setTitle("CosmoChat");
        primaryStage.setScene(scene);

        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        primaryStage.setWidth(Math.min(1280, bounds.getWidth() * 0.85));
        primaryStage.setHeight(Math.min(800, bounds.getHeight() * 0.85));
        primaryStage.centerOnScreen();

        primaryStage.show();
        starfield.startAnimation();
        primaryStage.setOnCloseRequest(e -> starfield.stopAnimation());
    }

    private HBox createTitleBar(Stage stage) {
        HBox titleBar = new HBox();
        titleBar.getStyleClass().add("title-bar");
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.setPadding(new Insets(0, 0, 0, 16));
        titleBar.setPrefHeight(40);
        Label title = new Label("CosmoChat");
        title.getStyleClass().add("title-bar-label");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button btnMinimize = createWindowButton("—", "title-btn-min");
        Button btnMaximize = createWindowButton("□", "title-btn-max");
        Button btnClose = createWindowButton("×", "title-btn-close");
        btnMinimize.setOnAction(e -> stage.setIconified(true));
        btnMaximize.setOnAction(e -> {
            if (isCustomMaximized) {
                stage.setX(prevX); stage.setY(prevY);
                stage.setWidth(prevW); stage.setHeight(prevH);
                isCustomMaximized = false;
            } else {
                prevX = stage.getX(); prevY = stage.getY();
                prevW = stage.getWidth(); prevH = stage.getHeight();
                Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
                stage.setX(bounds.getMinX()); stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth()); stage.setHeight(bounds.getHeight());
                isCustomMaximized = true;
            }
        });
        btnClose.setOnAction(e -> stage.close());
        buttons.getChildren().addAll(btnMinimize, btnMaximize, btnClose);
        titleBar.getChildren().addAll(title, spacer, buttons);
        titleBar.setOnMousePressed(event -> { if (!isCustomMaximized) { xOffset = event.getSceneX(); yOffset = event.getSceneY(); } });
        titleBar.setOnMouseDragged(event -> { if (!isCustomMaximized) { stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset); } });
        titleBar.setOnMouseClicked(event -> { if (event.getClickCount() == 2) btnMaximize.fire(); });
        return titleBar;
    }

    private Button createWindowButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().addAll("title-btn", styleClass);
        btn.setMinWidth(46); btn.setMinHeight(40);
        return btn;
    }

    public static void main(String[] args) { launch(args); }
}

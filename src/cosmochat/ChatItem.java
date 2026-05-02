package cosmochat;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ChatItem {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty title = new SimpleStringProperty();
    private final StringProperty date = new SimpleStringProperty();
    private final StringProperty iconGlyph = new SimpleStringProperty();
    private final BooleanProperty active = new SimpleBooleanProperty(false);
    
    // Список сообщений для этого чата
    private final ObservableList<ChatMessage> messages = FXCollections.observableArrayList();

    public ChatItem(int id, String title, String date, String iconGlyph) {
        this.id.set(id);
        this.title.set(title);
        this.date.set(date);
        this.iconGlyph.set(iconGlyph);
    }

    // Геттеры и свойства
    public int getId() { return id.get(); }
    public String getTitle() { return title.get(); }
    public void setTitle(String value) { title.set(value); }
    public StringProperty titleProperty() { return title; }
    public String getDate() { return date.get(); }
    public StringProperty dateProperty() { return date; }
    public String getIconGlyph() { return iconGlyph.get(); }
    public StringProperty iconGlyphProperty() { return iconGlyph; }
    public boolean isActive() { return active.get(); }
     public void setActive(boolean value) { active.set(value); }
     public BooleanProperty activeProperty() { return active; }
     public ObservableList<ChatMessage> getMessages() { return messages; }
     
     // Setter for updating ID after DB persistence (for new chats)
     public void setId(int value) { id.set(value); }
 }
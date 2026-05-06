package cosmochat;

public class ChatMessage {
    public enum Role { USER, AI }
    
    private final Role role;
    private final String text;
    private final String time;

    public ChatMessage(Role role, String text, String time) {
        this.role = role;
        this.text = text;
        this.time = time;
    }

    public Role getRole() { return role; }
    public String getText() { return text; }
    public String getTime() { return time; }
}
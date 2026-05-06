package cosmochat.domain.port;

public interface AiPort {
    String sendMessage(String message) throws Exception;
    boolean isAvailable();
}

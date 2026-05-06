package cosmochat.application.port;

import cosmochat.application.dto.SendMessageCommand;
import cosmochat.application.dto.SendMessageResult;

public interface SendMessage {
    SendMessageResult execute(SendMessageCommand command) throws Exception;
}

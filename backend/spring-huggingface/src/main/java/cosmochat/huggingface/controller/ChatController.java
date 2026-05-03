package cosmochat.huggingface.controller;

import cosmochat.huggingface.service.HuggingFaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ChatController {
    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    private final HuggingFaceService huggingFaceService;

    public ChatController(HuggingFaceService huggingFaceService) {
        this.huggingFaceService = huggingFaceService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> payload) {
        String message = (String) payload.get("message");
        Integer maxTokens = (Integer) payload.get("max_tokens");
        Double temperature = (Double) payload.get("temperature");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Message is required",
                "error_type", "BAD_REQUEST"
            ));
        }

        try {
            String response = huggingFaceService.generateResponse(message);
            return ResponseEntity.ok(Map.of(
                "response", response,
                "model", "Qwen2.5-Coder-7B-Instruct (HuggingFace)",
                "provider", "huggingface"
            ));
        } catch (Exception e) {
            logger.error("Chat error", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", e.getMessage(),
                "error_type", "UNKNOWN_ERROR"
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "huggingface-spring",
            "model", "Qwen2.5-Coder-7B-Instruct"
        ));
    }
}

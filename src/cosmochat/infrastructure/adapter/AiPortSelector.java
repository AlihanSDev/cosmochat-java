package cosmochat.infrastructure.adapter;

import cosmochat.domain.port.AiPort;

import java.util.HashMap;
import java.util.Map;

/**
 * AiPortSelector selects the appropriate AI client based on model name.
 * Allows runtime switching between different AI backends (local Python, HuggingFace, etc.).
 */
public class AiPortSelector implements AiPort {
    private final Map<String, AiPort> portMap;
    private final AiPort defaultPort;

    public AiPortSelector(Map<String, AiPort> portMap) {
        this.portMap = new HashMap<>(portMap);
        this.defaultPort = portMap.values().stream().findFirst().orElse(null);
    }

    /**
     * Returns the AiPort implementation for the given model name.
     * Falls back to default port if model not found.
     */
    public AiPort getPortForModel(String model) {
        AiPort port = portMap.get(model);
        if (port == null) {
            port = defaultPort;
        }
        return port;
    }

    /**
     * Register a new model-to-port mapping at runtime.
     */
    public void registerPort(String model, AiPort port) {
        portMap.put(model, port);
    }

    @Override
    public boolean isAvailable() {
        // Check if at least one port is available
        return portMap.values().stream()
                .anyMatch(AiPort::isAvailable);
    }

    @Override
    public String sendMessage(String message) throws Exception {
        throw new UnsupportedOperationException(
            "Use getPortForModel(String model).sendMessage() instead"
        );
    }
}

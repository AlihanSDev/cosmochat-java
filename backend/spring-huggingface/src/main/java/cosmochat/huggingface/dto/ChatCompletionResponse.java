package cosmochat.huggingface.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatCompletionResponse(
    @JsonProperty("id")
    String id,
    @JsonProperty("object")
    String object,
    @JsonProperty("created")
    Long created,
    @JsonProperty("model")
    String model,
    @JsonProperty("choices")
    List<Choice> choices,
    @JsonProperty("usage")
    Usage usage
) {
    public record Choice(
        @JsonProperty("index")
        Integer index,
        @JsonProperty("message")
        Message message,
        @JsonProperty("finish_reason")
        String finishReason
    ) {}

    public record Message(
        @JsonProperty("role")
        String role,
        @JsonProperty("content")
        String content
    ) {}

    public record Usage(
        @JsonProperty("prompt_tokens")
        Integer promptTokens,
        @JsonProperty("completion_tokens")
        Integer completionTokens,
        @JsonProperty("total_tokens")
        Integer totalTokens
    ) {}
}

package uni.bugtracker.backend.dto.event;

// only for creation

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import uni.bugtracker.backend.model.EventType;

import java.time.Instant;

@Data
public class EventRequest {
    @NotNull
    @Positive(message = "sessionId must be positive number")
    private Long sessionId;

    @NotNull
    private EventType type; // EventType должен выдать ошибку 4хх, если неподдерживаемый тип

    @NotBlank
    private String name;

    @NotBlank
    private String log;

    private String stackTrace;

    @NotBlank
//    @Pattern(regexp = "^https?://", message = "Url must begin with with http")
    private String url;

    private String element;

    @NotNull
    @PastOrPresent(message = "timestamp cannot be in the future.")
    private Instant timestamp;

    @Valid
    private MetadataPart metadata;

    @Data
    public static class MetadataPart {
        private String fileName;

        @Pattern(regexp = "^[0-9]*$", message = "Line number must contain only digits")
        private String lineNumber;

        @Pattern(regexp = "^[0-9]*$", message = "Status code must contain only digits")
        private String statusCode;
    }


}

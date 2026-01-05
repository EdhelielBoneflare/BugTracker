package uni.bugtracker.backend.dto.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.EventType;

import java.time.Instant;

@Data
@AllArgsConstructor
public class EventDetailsResponse {

    private Long eventId;
    private Long sessionId;
    private EventType type;
    private String name;
    private String log;
    private String stackTrace;
    private String url;
    private String element;
    private Instant timestamp;
    private Metadata metadata;

    public EventDetailsResponse(Event event) {
        this.eventId = event.getId();
        this.sessionId = event.getSession().getId();
        this.type = event.getType();
        this.name = event.getName();
        this.log = event.getLog();
        this.stackTrace = event.getStackTrace();
        this.url = event.getUrl();
        this.element = event.getElement();
        this.timestamp = event.getTimestamp();
        this.metadata = new Metadata();

        if (event.getMetadata() != null) {
            this.metadata.fileName = event.getMetadata().getFileName();
            this.metadata.lineNumber = event.getMetadata().getLineNumber();
            this.metadata.statusCode = event.getMetadata().getStatusCode();
        }
    }

    @Data
    public static class Metadata {
        private String fileName;
        private String lineNumber;
        private String statusCode;
    }


}

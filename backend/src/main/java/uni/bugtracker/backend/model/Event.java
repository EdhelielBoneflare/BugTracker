package uni.bugtracker.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;

@Data
@Entity
@Table(name = "event")
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Enumerated(EnumType.STRING)
    private EventType type;

    private String name;

    @Lob
    @Size(max = 4_000_000)
    private String log;

    @Lob
    @Size(max = 4_000_000)
    private String stackTrace;

    private String url;
    private String element; // For user actions
    private Instant timestamp;

    @Embedded
    private Metadata metadata;

    @Embeddable
    @Data
    @NoArgsConstructor(access = AccessLevel.PUBLIC)
    public static class Metadata {
        private String fileName;
        private String lineNumber;
        private String statusCode;
    }
}

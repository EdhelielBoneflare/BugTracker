package uni.bugtracker.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Table(name = "session")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    private Boolean isActive;

    @NotNull
    private Instant startTime;

    private Instant endTime;
    private String browser;
    private String browserVersion;
    private String os;
    private String deviceType;
    private String screenResolution;
    private String viewportSize;
    private String language;
    private String userAgent;
    private String ipAddress;
    private String cookiesHash; // Hashed for privacy

    @ElementCollection
    @CollectionTable(
            name = "session_plugins",
            joinColumns = @JoinColumn(name = "session_id")
    )
    @Column(name = "plugin")
    private List<String> plugins;
}

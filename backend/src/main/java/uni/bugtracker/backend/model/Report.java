package uni.bugtracker.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "report",
        indexes = {
                @Index(name = "idx_report_session", columnList = "sessionId"),
                @Index(name = "idx_report_project", columnList = "projectId")
        })
@Getter@Setter
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Size(max = 255)
    @Column(length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = Tag.class)
    @CollectionTable(name = "report_tags", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "tag")
    private List<Tag> tags;

    @NotNull
    private Instant reportedAt;

    @Size(max = 5000)
    @Column(length = 5000)
    private String comments;

    @Email
    @Size(max = 255)
    @Column(length = 255)
    private String userEmail;

//    @NotBlank // if our system composes all events, we cannot get screen
    @Lob // large object
    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)  // load the image only while referencing (saving memory when selecting entities)
    private String screen;

    @Size(max = 2048)
    @Column(nullable = false, length = 2048)
    private String currentUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "related_event_id")
    private List<Long> relatedEventIds;

    /**
     * true — report от пользователя
     * false — собран автоматически
     */
    @Column(nullable = false)
    private boolean userProvided = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "developer_id", nullable = true)
    private Developer developer;

    @Enumerated(EnumType.STRING)
//    @NotNull // connect DeepSeek API
    private CriticalityLevel criticality;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ReportStatus status = ReportStatus.NEW;

}

package uni.bugtracker.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "report")
@Getter@Setter
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank
    @Size(max = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @ElementCollection(targetClass = Tag.class)
    @CollectionTable(name = "report_tags", joinColumns = @JoinColumn(name = "report_id"))
    @Column(name = "tag")
    private List<Tag> tags;

    @NotNull
    private Instant date;

    @Size(max = 5000)
    @Column(length = 5000)
    private String comments;

    @NotBlank
    @Size(max = 4_000_000)
    @Column(length = 4_000_000)
    private String log;

    @NotBlank
    @Size(max = 20000)
    @Column(length = 20000)
    private String actions;

    @NotBlank
    @Lob // large object
    @Column(columnDefinition = "TEXT")
    @Basic(fetch = FetchType.LAZY)  // load the image only while referencing (saving memory when selecting entities)
    private String screen;

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

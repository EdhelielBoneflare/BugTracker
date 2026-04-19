package uni.bugtracker.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="dev_notif")
public class DeveloperNotification {

    public enum State {
        REGISTERING,
        ACTIVE,
        DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @NotBlank
    @Column(name="dev_id", nullable = false)
    private String devId;

    @NotNull
    @NotBlank
    @Column(name="project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeveloperNotification.State state;

    @Column(name="chat_id")
    private Long chatId;

    @NotNull
    @Column(name="reg_token", unique = true, nullable = false)
    private String regToken;

    @NotNull
    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}

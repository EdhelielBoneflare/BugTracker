package uni.bugtracker.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uni.bugtracker.backend.security.model.Role;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@Table(name = "user_dev")
@NoArgsConstructor
@AllArgsConstructor
public class Developer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = false)
    @Size(max = 255)
    private String username;

    @Column(nullable = false)
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "developer_project", // the table that contains key pairs
            joinColumns = @JoinColumn(name = "developer_id"), // FK referring to the current entity
            inverseJoinColumns = @JoinColumn(name = "project_id") // FK for the related party
    )
    private Set<Project> projects = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role = Role.DEVELOPER;

}

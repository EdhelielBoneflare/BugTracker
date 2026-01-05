package uni.bugtracker.backend.security.dto;

import lombok.Getter;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.security.model.Role;

import java.util.Set;
import java.util.stream.Collectors;


@Getter
public class UserDTO {
    private String id;
    private String username;
    private Role role;
    private Set<String> projectIds;

    public UserDTO (Developer user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.role = user.getRole();
        this.projectIds = user.getProjects()
                .stream()
                .map(Project::getId)
                .collect(Collectors.toSet());
    }

}

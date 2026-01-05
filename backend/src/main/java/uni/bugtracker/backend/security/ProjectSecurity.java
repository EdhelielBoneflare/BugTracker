package uni.bugtracker.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.security.model.Role;

@Component("projectSecurity")
@RequiredArgsConstructor
public class ProjectSecurity {

    private final DeveloperRepository developerRepository;

    public boolean hasAccessToProject(String projectId, Authentication auth) {
        CustomUserDetails user = (CustomUserDetails) auth.getPrincipal();

        if (user.getRole() == Role.ADMIN) {
            return true;
        }

        return developerRepository.existsByIdAndProjects_Id(
                user.getId(),
                projectId
        );
    }
}

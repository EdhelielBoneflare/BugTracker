package uni.bugtracker.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uni.bugtracker.backend.dto.project.ProjectRequestBody;
import uni.bugtracker.backend.exception.ProjectCreationError;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.security.CustomUserDetails;
import uni.bugtracker.backend.security.model.Role;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final DeveloperRepository developerRepository;

    @Transactional
    public Project createProject(ProjectRequestBody projectBody, Authentication authentication) {
        String username = authentication.getName();
        Developer currentUser = developerRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Role currentRole = currentUser.getRole();

        if (currentRole.equals(Role.DEVELOPER)
            && !developerRepository.findProjectsByDeveloperId(currentUser.getId()).isEmpty()) {
            throw new ProjectCreationError("You have active projects in dev role. " +
                "Create a new account to manage projects as pm.");
        }

        Project project = new Project();
        project.setName(projectBody.getProjectName());
        Project savedProject = projectRepository.save(project);

        if (currentRole != Role.ADMIN) {
            if (currentRole.equals(Role.DEVELOPER)) {
                currentUser.setRole(Role.PM);
            }
            currentUser.getProjects().add(savedProject);
            developerRepository.save(currentUser);
        }
        return savedProject;
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public List<Project> getProjectsForCurrentUser(Authentication authentication) {

        CustomUserDetails user =
                (CustomUserDetails) authentication.getPrincipal();

        // ADMIN -> all projects
        if (user.getRole() == Role.ADMIN) {
            return projectRepository.findAll();
        }

        // PM / DEV -> only their projects
        return developerRepository.findProjectsByDeveloperId(user.getId());
    }

    public Project updateProject(String projectId, ProjectRequestBody projectBody, Authentication authentication) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id:" + projectId));

        String username = authentication.getName();
        Developer currentUser = developerRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Role currentRole = currentUser.getRole();

        if (currentRole != Role.ADMIN) {
            // if not PM -> refuse
            if (currentRole != Role.PM) {
                throw new AccessDeniedException("You are not allowed to update projects");
            }

            // if PM -> check that's its project
            boolean hasAccess = currentUser.getProjects().stream()
                .anyMatch(p -> p.getId().equals(projectId));
            if (!hasAccess) {
                throw new AccessDeniedException("You can update only your projects");
            }
        }

        if (projectBody.getProjectName() != null)
            project.setName(projectBody.getProjectName());
        return projectRepository.save(project);
    }

    public Project deleteProject(String projectId, Authentication authentication) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new ResourceNotFoundException("Project not found with id:" + projectId));

        String username = authentication.getName();
        Developer currentUser = developerRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        Role currentRole = currentUser.getRole();

        if (currentRole != Role.ADMIN) {
            // if not PM -> refuse
            if (currentRole != Role.PM) {
                throw new AccessDeniedException("You are not allowed to delete projects");
            }

            // if PM -> check that's its project
            boolean hasAccess = currentUser.getProjects().stream()
                .anyMatch(p -> p.getId().equals(projectId));
            if (!hasAccess) {
                throw new AccessDeniedException("You can delete only your projects");
            }
        }
        List<Developer> devs = developerRepository.findAllByProjects_Id(projectId);
        for (Developer dev : devs) {
            dev.getProjects().remove(project);
        }
        developerRepository.saveAll(devs);

        projectRepository.deleteById(projectId);
        return project;
    }
}

package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import uni.bugtracker.backend.dto.project.ProjectRequestBody;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.security.CustomUserDetails;
import uni.bugtracker.backend.security.model.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProjectService projectService;

    private Project project;
    private CustomUserDetails adminUser;
    private CustomUserDetails devUser;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId("project-123");
        project.setName("Test Project");

        Developer adminDeveloper = Developer.builder()
                .id("admin-id")
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .projects(new HashSet<>())
                .build();

        Developer devDeveloper = Developer.builder()
                .id("dev-id")
                .username("dev")
                .password("password")
                .role(Role.DEVELOPER)
                .projects(new HashSet<>())
                .build();

        adminUser = new CustomUserDetails(adminDeveloper);
        devUser = new CustomUserDetails(devDeveloper);
    }

    @Test
    void createProject_shouldSaveAndReturnProject() {
        // Given
        ProjectRequestBody request = new ProjectRequestBody();
        request.setProjectName("New Project");

        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId("new-id");
            p.setName("New Project");
            return p;
        });

        // When
        Project result = projectService.createProject(request);

        // Then
        assertThat(result.getId()).isEqualTo("new-id");
        assertThat(result.getName()).isEqualTo("New Project");
        verify(projectRepository).save(any(Project.class));
    }

    @Test
    void getAllProjects_shouldReturnAllProjects() {
        // Given
        List<Project> projects = List.of(project);
        when(projectRepository.findAll()).thenReturn(projects);

        // When
        List<Project> result = projectService.getAllProjects();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Test Project");
    }

    @Test
    void getProjectsForCurrentUser_whenAdmin_shouldReturnAllProjects() {
        // Given
        when(authentication.getPrincipal()).thenReturn(adminUser);

        List<Project> projects = List.of(project);
        when(projectRepository.findAll()).thenReturn(projects);

        // When
        List<Project> result = projectService.getProjectsForCurrentUser(authentication);

        // Then
        assertThat(result).hasSize(1);
        verify(projectRepository).findAll();
        verifyNoInteractions(developerRepository);
    }

    @Test
    void getProjectsForCurrentUser_whenDeveloper_shouldReturnAssignedProjects() {
        // Given
        when(authentication.getPrincipal()).thenReturn(devUser);

        List<Project> assignedProjects = List.of(project);
        when(developerRepository.findProjectsByDeveloperId("dev-id")).thenReturn(assignedProjects);

        // When
        List<Project> result = projectService.getProjectsForCurrentUser(authentication);

        // Then
        assertThat(result).hasSize(1);
        verify(developerRepository).findProjectsByDeveloperId("dev-id");
        verify(projectRepository, never()).findAll();
    }

    @Test
    void getProjectsForCurrentUser_whenProjectManager_shouldReturnAssignedProjects() {
        // Given
        Developer pmDeveloper = Developer.builder()
                .id("pm-id")
                .username("pm")
                .password("password")
                .role(Role.PM)
                .projects(new HashSet<>())
                .build();

        CustomUserDetails pmUser = new CustomUserDetails(pmDeveloper);
        when(authentication.getPrincipal()).thenReturn(pmUser);

        List<Project> assignedProjects = List.of(project);
        when(developerRepository.findProjectsByDeveloperId("pm-id")).thenReturn(assignedProjects);

        // When
        List<Project> result = projectService.getProjectsForCurrentUser(authentication);

        // Then
        assertThat(result).hasSize(1);
        verify(developerRepository).findProjectsByDeveloperId("pm-id");
        verify(projectRepository, never()).findAll();
    }

    @Test
    void updateProject_shouldUpdateNameAndSave() {
        // Given
        ProjectRequestBody request = new ProjectRequestBody();
        request.setProjectName("Updated Name");

        when(projectRepository.findById("project-123")).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        // When
        Project result = projectService.updateProject("project-123", request);

        // Then
        assertThat(result.getName()).isEqualTo("Updated Name");
        verify(projectRepository).findById("project-123");
        verify(projectRepository).save(project);
    }

    @Test
    void updateProject_whenProjectNotFound_shouldThrowException() {
        // Given
        when(projectRepository.findById(anyString())).thenReturn(Optional.empty());
        ProjectRequestBody request = new ProjectRequestBody();
        request.setProjectName("Name");

        // When & Then
        assertThatThrownBy(() -> projectService.updateProject("invalid", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void updateProject_withNullProjectName_shouldNotUpdateName() {
        // Given
        project.setName("Original Name");

        ProjectRequestBody request = new ProjectRequestBody();
        request.setProjectName(null);

        when(projectRepository.findById("project-123")).thenReturn(Optional.of(project));
        when(projectRepository.save(project)).thenReturn(project);

        // When
        Project result = projectService.updateProject("project-123", request);

        // Then
        assertThat(result.getName()).isEqualTo("Original Name");
        verify(projectRepository).save(project);
    }

    @Test
    void deleteProject_shouldDeleteAndReturnProject() {
        // Given
        when(projectRepository.findById("project-123")).thenReturn(Optional.of(project));

        // When
        Project result = projectService.deleteProject("project-123");

        // Then
        assertThat(result).isEqualTo(project);
        verify(projectRepository).findById("project-123");
        verify(projectRepository).deleteById("project-123");
    }

    @Test
    void deleteProject_whenProjectNotFound_shouldThrowException() {
        // Given
        when(projectRepository.findById(anyString())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> projectService.deleteProject("invalid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
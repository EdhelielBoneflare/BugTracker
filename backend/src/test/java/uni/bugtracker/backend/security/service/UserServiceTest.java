package uni.bugtracker.backend.security.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Developer;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.security.CustomUserDetails;
import uni.bugtracker.backend.security.ProjectSecurity;
import uni.bugtracker.backend.security.dto.UserDTO;
import uni.bugtracker.backend.security.model.Role;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectSecurity projectSecurity;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    private Developer testUser;
    private Developer targetUser;
    private Project testProject;
    private CustomUserDetails adminUser;
    private CustomUserDetails pmUser;
    private CustomUserDetails devUser;

    @BeforeEach
    void setUp() {
        testUser = Developer.builder()
                .id("user-123")
                .username("testuser")
                .password("password")
                .role(Role.DEVELOPER)
                .projects(new HashSet<>())
                .build();

        targetUser = Developer.builder()
                .id("target-456")
                .username("targetuser")
                .password("password")
                .role(Role.DEVELOPER)
                .projects(new HashSet<>())
                .build();

        testProject = new Project();
        testProject.setId("project-789");
        testProject.setName("Test Project");

        Developer adminDev = Developer.builder()
                .id("admin-id")
                .username("admin")
                .password("password")
                .role(Role.ADMIN)
                .build();

        Developer pmDev = Developer.builder()
                .id("pm-id")
                .username("pmuser")
                .password("password")
                .role(Role.PM)
                .build();

        Developer devDev = Developer.builder()
                .id("dev-id")
                .username("devuser")
                .password("password")
                .role(Role.DEVELOPER)
                .build();

        adminUser = new CustomUserDetails(adminDev);
        pmUser = new CustomUserDetails(pmDev);
        devUser = new CustomUserDetails(devDev);
    }

    @Test
    void changeUserRole_ShouldUpdateRoleSuccessfully() {
        // Given
        String userId = "user-123";
        Role newRole = Role.PM;

        when(developerRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(developerRepository.save(any(Developer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        UserDTO result = userService.changeUserRole(userId, newRole);

        // Then
        assertThat(result.getRole()).isEqualTo(newRole);
        assertThat(testUser.getRole()).isEqualTo(newRole);
        verify(developerRepository).save(testUser);
    }

    @Test
    void changeUserRole_ShouldThrowExceptionWhenUserNotFound() {
        // Given
        String userId = "non-existent";
        when(developerRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changeUserRole(userId, Role.PM))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User nor found"); // Fixed to match actual error
    }

    @Test
    void getAllUsers_ShouldReturnPagedUserDTOs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Developer> developers = Arrays.asList(testUser, targetUser);
        Page<Developer> developerPage = new PageImpl<>(developers, pageable, developers.size());

        when(developerRepository.findAll(pageable)).thenReturn(developerPage);

        // When
        Page<UserDTO> result = userService.getAllUsers(pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo(testUser.getUsername());
        assertThat(result.getContent().get(1).getUsername()).isEqualTo(targetUser.getUsername());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void assignProject_AdminCanAssignAnyProject() {
        // Given
        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectRepository.findById("project-789")).thenReturn(Optional.of(testProject));
        when(developerRepository.save(any(Developer.class))).thenReturn(targetUser);

        // When
        UserDTO result = userService.assignProject("target-456", "project-789", authentication);

        // Then
        assertThat(result.getProjectIds()).contains(testProject.getId());
        verify(projectSecurity, never()).hasAccessToProject(anyString(), any());
    }

    @Test
    void assignProject_PMCanAssignOnlyTheirProjects() {
        // Given
        when(authentication.getPrincipal()).thenReturn(pmUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectRepository.findById("project-789")).thenReturn(Optional.of(testProject));
        when(projectSecurity.hasAccessToProject("project-789", authentication)).thenReturn(true);
        when(developerRepository.save(any(Developer.class))).thenReturn(targetUser);

        // When
        UserDTO result = userService.assignProject("target-456", "project-789", authentication);

        // Then
        assertThat(result.getProjectIds()).contains(testProject.getId());
        verify(projectSecurity).hasAccessToProject("project-789", authentication);
    }

    @Test
    void assignProject_PMCannotAssignOtherProjects() {
        // Given
        when(authentication.getPrincipal()).thenReturn(pmUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectSecurity.hasAccessToProject("project-789", authentication)).thenReturn(false);
        // Note: projectRepository.findById is NOT mocked since exception is thrown before it's called

        // When & Then
        assertThatThrownBy(() -> userService.assignProject("target-456", "project-789", authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("no rights");

        verify(projectRepository, never()).findById(anyString());
    }

    @Test
    void assignProject_DeveloperCannotAssignProjects() {
        // Given
        when(authentication.getPrincipal()).thenReturn(devUser);

        // When & Then
        assertThatThrownBy(() -> userService.assignProject("target-456", "project-789", authentication))
                .isInstanceOf(AccessDeniedException.class);

        verify(developerRepository, never()).findById(anyString());
        verify(projectRepository, never()).findById(anyString());
    }

    @Test
    void assignProject_ShouldBeIdempotentWhenUserAlreadyHasProject() {
        // Given
        targetUser.getProjects().add(testProject);

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectRepository.findById("project-789")).thenReturn(Optional.of(testProject));

        // When
        UserDTO result = userService.assignProject("target-456", "project-789", authentication);

        // Then
        assertThat(result.getProjectIds()).contains(testProject.getId());
        verify(developerRepository, never()).save(any());
    }

    @Test
    void assignProject_ShouldThrowWhenUserNotFound() {
        // Given
        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(developerRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.assignProject("non-existent", "project-789", authentication))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(projectRepository, never()).findById(anyString());
    }

    @Test
    void assignProject_ShouldThrowWhenProjectNotFound() {
        // Given
        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectRepository.findById("non-existent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.assignProject("target-456", "non-existent", authentication))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeProject_ShouldRemoveProjectSuccessfully() {
        // Given
        targetUser.getProjects().add(testProject);

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectRepository.findById("project-789")).thenReturn(Optional.of(testProject));
        when(developerRepository.save(any(Developer.class))).thenReturn(targetUser);

        // When
        UserDTO result = userService.removeProject("target-456", "project-789", authentication);

        // Then
        assertThat(result.getProjectIds()).doesNotContain(testProject.getId());
        verify(developerRepository).save(targetUser);
    }

    @Test
    void removeProject_ShouldBeIdempotentWhenUserDoesNotHaveProject() {
        // Given
        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(developerRepository.findById("target-456")).thenReturn(Optional.of(targetUser));
        when(projectRepository.findById("project-789")).thenReturn(Optional.of(testProject));

        // When
        UserDTO result = userService.removeProject("target-456", "project-789", authentication);

        // Then
        assertThat(result.getProjectIds()).doesNotContain(testProject.getId());
        verify(developerRepository, never()).save(any());
    }

    @Test
    void removeProject_PMCannotRemoveFromOtherProjects() {
        // Given
        when(authentication.getPrincipal()).thenReturn(pmUser);
        when(projectSecurity.hasAccessToProject("project-789", authentication)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.removeProject("target-456", "project-789", authentication))
                .isInstanceOf(AccessDeniedException.class);

        verify(developerRepository, never()).findById(anyString());
        verify(projectRepository, never()).findById(anyString());
    }
}
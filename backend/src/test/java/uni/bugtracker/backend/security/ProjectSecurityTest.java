package uni.bugtracker.backend.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import uni.bugtracker.backend.repository.DeveloperRepository;
import uni.bugtracker.backend.security.model.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectSecurityTest {

    @Mock
    private DeveloperRepository developerRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProjectSecurity projectSecurity;

    @Test
    void hasAccessToProject_whenAdmin_shouldReturnTrue() {
        // Given
        String projectId = "project-123";
        CustomUserDetails adminUser = mock(CustomUserDetails.class);

        when(authentication.getPrincipal()).thenReturn(adminUser);
        when(adminUser.getRole()).thenReturn(Role.ADMIN);

        // When
        boolean hasAccess = projectSecurity.hasAccessToProject(projectId, authentication);

        // Then
        assertThat(hasAccess).isTrue();
        verifyNoInteractions(developerRepository);
    }

    @Test
    void hasAccessToProject_whenDeveloperHasAccess_shouldReturnTrue() {
        // Given
        String projectId = "project-123";
        String userId = "dev-456";

        CustomUserDetails developer = mock(CustomUserDetails.class);
        when(authentication.getPrincipal()).thenReturn(developer);
        when(developer.getRole()).thenReturn(Role.DEVELOPER);
        when(developer.getId()).thenReturn(userId);

        when(developerRepository.existsByIdAndProjects_Id(eq(userId), eq(projectId)))
                .thenReturn(true);

        // When
        boolean hasAccess = projectSecurity.hasAccessToProject(projectId, authentication);

        // Then
        assertThat(hasAccess).isTrue();
        verify(developerRepository).existsByIdAndProjects_Id(userId, projectId);
    }

    @Test
    void hasAccessToProject_whenDeveloperNoAccess_shouldReturnFalse() {
        // Given
        String projectId = "project-123";
        String userId = "dev-456";

        CustomUserDetails developer = mock(CustomUserDetails.class);
        when(authentication.getPrincipal()).thenReturn(developer);
        when(developer.getRole()).thenReturn(Role.PM);
        when(developer.getId()).thenReturn(userId);

        when(developerRepository.existsByIdAndProjects_Id(eq(userId), eq(projectId)))
                .thenReturn(false);

        // When
        boolean hasAccess = projectSecurity.hasAccessToProject(projectId, authentication);

        // Then
        assertThat(hasAccess).isFalse();
        verify(developerRepository).existsByIdAndProjects_Id(userId, projectId);
    }

    @Test
    void hasAccessToProject_whenManager_shouldCheckRepository() {
        // Given
        String projectId = "project-789";
        String userId = "mgr-001";

        CustomUserDetails manager = mock(CustomUserDetails.class);
        when(authentication.getPrincipal()).thenReturn(manager);
        when(manager.getRole()).thenReturn(Role.PM);
        when(manager.getId()).thenReturn(userId);

        when(developerRepository.existsByIdAndProjects_Id(userId, projectId))
                .thenReturn(true);

        // When
        boolean hasAccess = projectSecurity.hasAccessToProject(projectId, authentication);

        // Then
        assertThat(hasAccess).isTrue();
        verify(developerRepository).existsByIdAndProjects_Id(userId, projectId);
    }

    @Test
    void hasAccessToProject_withEmptyProjectId_shouldCheckRepository() {
        // Given
        String emptyProjectId = "";
        String userId = "dev-123";

        CustomUserDetails user = mock(CustomUserDetails.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(user.getRole()).thenReturn(Role.DEVELOPER);
        when(user.getId()).thenReturn(userId);

        when(developerRepository.existsByIdAndProjects_Id(userId, emptyProjectId))
                .thenReturn(false);

        // When
        boolean hasAccess = projectSecurity.hasAccessToProject(emptyProjectId, authentication);

        // Then
        assertThat(hasAccess).isFalse();
        verify(developerRepository).existsByIdAndProjects_Id(userId, emptyProjectId);
    }
}
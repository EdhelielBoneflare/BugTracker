package uni.bugtracker.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uni.bugtracker.backend.dto.session.SessionRequest;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.model.Session;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.repository.SessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void createSession_shouldSaveAndReturnResponse() {
        // Given
        SessionRequest request = createSessionRequest();
        Project project = new Project();
        project.setId("project-123");

        when(projectRepository.findById("project-123")).thenReturn(Optional.of(project));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(100L);
            return session;
        });

        // When
        var result = sessionService.createSession(request);

        // Then
        assertThat(result.getMessage()).contains("Session created");
        assertThat(result.getSessionId()).isEqualTo(100L);
        verify(projectRepository).findById("project-123");
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void getSession_shouldReturnSessionDetails() {
        // Given
        Session session = new Session();
        session.setId(100L);
        Project project = new Project();
        project.setId("project-123");
        session.setProject(project);

        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        // When
        var result = sessionService.getSession(100L);

        // Then
        assertThat(result.getSessionId()).isEqualTo(100L);
        assertThat(result.getProjectId()).isEqualTo("project-123");
    }

    @Test
    void getSession_whenNotFound_shouldThrowException() {
        // Given
        when(sessionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> sessionService.getSession(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private SessionRequest createSessionRequest() {
        SessionRequest request = new SessionRequest();
        request.setProjectId("project-123");
        request.setStartTime(Instant.now());
        request.setBrowser("Chrome");
        request.setBrowserVersion("120");
        request.setOs("Windows");
        request.setDeviceType("desktop");
        request.setScreenResolution("1920x1080");
        request.setLanguage("en-US");
        request.setUserAgent("Mozilla");
        request.setIpAddress("192.168.1.1");
        request.setPlugins(List.of("Plugin1"));
        return request;
    }
}
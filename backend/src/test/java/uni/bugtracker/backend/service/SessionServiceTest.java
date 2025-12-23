package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
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
import java.util.Arrays;
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

    private SessionRequest sessionRequest;
    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        sessionRequest = new SessionRequest();
        sessionRequest.setProjectId(1L);
        sessionRequest.setStartTime(Instant.now());
        sessionRequest.setBrowser("Chrome");
        sessionRequest.setBrowserVersion("120.0.0.0");
        sessionRequest.setOs("Windows 10");
        sessionRequest.setDeviceType("Desktop");
        sessionRequest.setPlugins(Arrays.asList("Plugin1", "Plugin2"));
    }

    @Test
    void createSession_WithValidRequest_ShouldCreateAndReturnResponse() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Session savedSession = new Session();
        savedSession.setId(100L);
        savedSession.setProject(project);
        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // Act
        var response = sessionService.createSession(sessionRequest);

        // Assert
        assertThat(response.getMessage()).isEqualTo("Session created successfully");
        assertThat(response.getSessionId()).isEqualTo(100L);

        verify(projectRepository).findById(1L);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void createSession_WithNonExistentProject_ShouldThrowException() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionService.createSession(sessionRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Project not found");

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    void createSession_ShouldSetIsActiveToTrue() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Session savedSession = new Session();
        savedSession.setId(100L);
        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // Act
        sessionService.createSession(sessionRequest);

        // Assert
        verify(sessionRepository).save(argThat(session ->
                Boolean.TRUE.equals(session.getIsActive())
        ));
    }

    @Test
    void createSession_ShouldCopyAllFieldsFromRequest() {
        // Arrange
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        sessionRequest.setIpAddress("192.168.1.1");
        sessionRequest.setCookiesHash("abc123");
        sessionRequest.setLanguage("en-US");
        sessionRequest.setUserAgent("Mozilla/5.0");

        Session savedSession = new Session();
        savedSession.setId(100L);
        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // Act
        sessionService.createSession(sessionRequest);

        // Assert
        verify(sessionRepository).save(argThat(session ->
                session.getProject() != null &&
                        "Chrome".equals(session.getBrowser()) &&
                        "192.168.1.1".equals(session.getIpAddress()) &&
                        "abc123".equals(session.getCookiesHash()) &&
                        session.getPlugins() != null &&
                        session.getPlugins().size() == 2
        ));
    }

    @Test
    void getSession_WithExistingId_ShouldReturnResponse() {
        // Arrange
        Session session = new Session();
        session.setId(100L);
        session.setProject(project);
        session.setIsActive(true);

        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));

        // Act
        var response = sessionService.getSession(100L);

        // Assert
        assertThat(response).isNotNull();
        verify(sessionRepository).findById(100L);
    }

    @Test
    void getSession_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(sessionRepository.findById(100L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionService.getSession(100L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");
    }
}
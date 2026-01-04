package uni.bugtracker.backend.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uni.bugtracker.backend.dto.session.SessionCreationResponse;
import uni.bugtracker.backend.dto.session.SessionDetailsResponse;
import uni.bugtracker.backend.dto.session.SessionRequest;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.model.Session;
import uni.bugtracker.backend.repository.ProjectRepository;
import uni.bugtracker.backend.repository.SessionRepository;


@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final ProjectRepository projectRepository;

    @Transactional
    public SessionCreationResponse createSession(SessionRequest request) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Project not found: " + request.getProjectId()
                        )
                );

        Session session = new Session();

        session.setProject(project);
        session.setIsActive(true);
        session.setStartTime(request.getStartTime());
        session.setBrowser(request.getBrowser());
        session.setBrowserVersion(request.getBrowserVersion());
        session.setOs(request.getOs());
        session.setDeviceType(request.getDeviceType());
        session.setScreenResolution(request.getScreenResolution());
        session.setViewportSize(request.getViewportSize());
        session.setLanguage(request.getLanguage());
        session.setUserAgent(request.getUserAgent());
        session.setIpAddress(request.getIpAddress());
        session.setCookiesHash(request.getCookiesHash());
        session.setPlugins(request.getPlugins());

        Session sessionCreated = sessionRepository.save(session);

        return new SessionCreationResponse(
                "Session created successfully",
                sessionCreated.getId()
        );
    }

    public SessionDetailsResponse getSession(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        return new SessionDetailsResponse(session);
    }

    public String getProjectIdBySessionId(Long sessionId) {
        Session session =  sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        return session.getProject().getId();
    }
}

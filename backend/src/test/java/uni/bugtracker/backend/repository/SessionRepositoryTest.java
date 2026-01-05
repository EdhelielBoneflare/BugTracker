package uni.bugtracker.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.model.Session;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SessionRepositoryTest {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Project project;
    private Instant now;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        project = new Project();
        project.setName("Test Project");
        projectRepository.save(project);
    }

    @Test
    void findExpiredSessions_shouldReturnOnlyActiveExpiredSessions() {
        // Arrange: creating 3 sessions
        // 1. active, not overdue
        Session freshSession = createSession(true, now.minus(5, ChronoUnit.MINUTES), null);

        // 2. active, overdue
        Session expiredSession = createSession(true, now.minus(2, ChronoUnit.HOURS), null);

        // 3. not active
        Session inactiveSession = createSession(false, now.minus(3, ChronoUnit.HOURS), now.minus(1, ChronoUnit.HOURS));

        // deadline - 1 hour
        Instant deadline = now.minus(1, ChronoUnit.HOURS);

        // Act
        List<Session> expiredSessions = sessionRepository.findExpiredSessions(deadline);

        // Assert
        assertThat(expiredSessions)
                .hasSize(1)
                .containsExactly(expiredSession)
                .doesNotContain(freshSession, inactiveSession);
    }

    @Test
    void findExpiredSessions_whenNoExpiredSessions_shouldReturnEmptyList() {
        // Arrange: only not overdue sessions
        createSession(true, now.minus(10, ChronoUnit.MINUTES), null);
        createSession(true, now.minus(5, ChronoUnit.MINUTES), null);

        Instant deadline = now.minus(1, ChronoUnit.HOURS);

        // Act
        List<Session> result = sessionRepository.findExpiredSessions(deadline);

        // Assert
        assertThat(result).isEmpty();
    }

    private Session createSession(Boolean isActive, Instant startTime, Instant endTime) {
        Session session = new Session();
        session.setProject(project);
        session.setIsActive(isActive);
        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setBrowser("Chrome");
        session.setUserAgent("Test Agent");

        return sessionRepository.save(session);
    }
}
package uni.bugtracker.backend.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import uni.bugtracker.backend.model.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EventRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private ProjectRepository projectRepository;

    private Session session;
    private Project project;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setName("Test Project");
        projectRepository.save(project);

        session = new Session();
        session.setProject(project);
        session.setIsActive(true);
        session.setStartTime(Instant.now());
        sessionRepository.save(session);
    }

    @Test
    void findAllBySessionId_shouldReturnAllEventsForSession() {
        // Arrange
        Event event1 = createEvent(session, EventType.ERROR, Instant.now());
        Event event2 = createEvent(session, EventType.ACTION, Instant.now().plusSeconds(10));

        Session otherSession = createOtherSession();
        createEvent(otherSession, EventType.ERROR, Instant.now());

        // Act
        List<Event> events = eventRepository.findAllBySessionId(session.getId());

        // Assert
        assertThat(events).hasSize(2).containsExactly(event1, event2);
    }

    @Test
    void findFirstBySessionIdOrderByTimestampDesc_shouldReturnLatestEvent() {
        // Arrange
        Instant now = Instant.now();
        Event latest = createEvent(session, EventType.PERFORMANCE, now.minusSeconds(10));

        // Act
        Optional<Event> result = eventRepository.findFirstBySessionIdOrderByTimestampDesc(session.getId());

        // Assert
        assertThat(result).isPresent().contains(latest);
    }

    @Test
    void existsBySessionIdAndType_shouldReturnTrueWhenExists() {
        // Arrange
        createEvent(session, EventType.ERROR, Instant.now());

        // Act & Assert
        assertThat(eventRepository.existsBySessionIdAndType(session.getId(), EventType.ERROR)).isTrue();
        assertThat(eventRepository.existsBySessionIdAndType(session.getId(), EventType.ACTION)).isFalse();
    }

    @Test
    void deleteBySessionId_shouldDeleteAllEventsForSession() {
        // Arrange
        createEvent(session, EventType.ERROR, Instant.now());
        createEvent(session, EventType.ACTION, Instant.now());

        Session otherSession = createOtherSession();
        Event otherEvent = createEvent(otherSession, EventType.ERROR, Instant.now());

        // Act
        eventRepository.deleteBySessionId(session.getId());

        // Assert
        assertThat(eventRepository.findAllBySessionId(session.getId())).isEmpty();
        assertThat(eventRepository.findById(otherEvent.getId())).isPresent();
    }

    private Event createEvent(Session session, EventType type, Instant timestamp) {
        Event event = new Event();
        event.setSession(session);
        event.setType(type);
        event.setTimestamp(timestamp);
        event.setName("Test Event");

        return eventRepository.save(event);
    }

    private Session createOtherSession() {
        Session otherSession = new Session();
        otherSession.setProject(project);
        otherSession.setIsActive(false);
        otherSession.setStartTime(Instant.now());
        return sessionRepository.save(otherSession);
    }
}
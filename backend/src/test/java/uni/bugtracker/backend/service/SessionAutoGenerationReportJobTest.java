package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uni.bugtracker.backend.config.SessionProperties;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.EventType;
import uni.bugtracker.backend.model.Report;
import uni.bugtracker.backend.model.Session;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.ReportRepository;
import uni.bugtracker.backend.repository.SessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionAutoGenerationReportJobTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private SessionProperties sessionProperties;

    @InjectMocks
    private SessionAutoGenerationReportJob job;

    @BeforeEach
    void setUp() {
        when(sessionProperties.getLiveTimeout()).thenReturn(Duration.ofMinutes(30));
    }

    @Test
    void handleExpiredSessions_WithErrorEvents_ShouldCreateReport() {
        // Arrange
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);

        List<Session> expiredSessions = List.of(session);
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(expiredSessions);

        when(eventRepository.existsBySessionIdAndType(1L, EventType.ERROR))
                .thenReturn(true);

        List<Event> events = Arrays.asList(new Event(), new Event());
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);

        Event lastEvent = new Event();
        lastEvent.setTimestamp(Instant.now().minus(Duration.ofMinutes(10)));
        when(eventRepository.findFirstBySessionIdOrderByTimestampDesc(1L))
                .thenReturn(Optional.of(lastEvent));

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(sessionRepository).findExpiredSessions(any(Instant.class));
        verify(eventRepository).existsBySessionIdAndType(1L, EventType.ERROR);
        verify(eventRepository, never()).deleteBySessionId(1L);
        verify(reportRepository).save(any(Report.class));

        // Checking that session was deactivated
        verify(session).setIsActive(false);
        verify(session).setEndTime(lastEvent.getTimestamp());
    }

    @Test
    void handleExpiredSessions_WithoutErrorEvents_ShouldDeleteEvents() {
        // Arrange
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);

        List<Session> expiredSessions = List.of(session);
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(expiredSessions);

        when(eventRepository.existsBySessionIdAndType(1L, EventType.ERROR))
                .thenReturn(false);

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(eventRepository).deleteBySessionId(1L);
        verify(reportRepository, never()).save(any(Report.class));

        // Checking that session was deactivated
        verify(session).setIsActive(false);
    }

    @Test
    void handleExpiredSessions_WithNoLastEvent_ShouldUseCurrentTime() {
        // Arrange
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);

        List<Session> expiredSessions = List.of(session);
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(expiredSessions);

        when(eventRepository.existsBySessionIdAndType(1L, EventType.ERROR))
                .thenReturn(false);

        when(eventRepository.findFirstBySessionIdOrderByTimestampDesc(1L))
                .thenReturn(Optional.empty());

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(session).setIsActive(false);
        verify(session).setEndTime(any(Instant.class)); // Проверяем, что установлено какое-то время
    }

    @Test
    void handleExpiredSessions_WithMultipleSessions_ShouldProcessAll() {
        // Arrange
        Session session1 = mock(Session.class);
        when(session1.getId()).thenReturn(1L);

        Session session2 = mock(Session.class);
        when(session2.getId()).thenReturn(2L);

        List<Session> expiredSessions = Arrays.asList(session1, session2);
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(expiredSessions);

        when(eventRepository.existsBySessionIdAndType(anyLong(), eq(EventType.ERROR)))
                .thenReturn(true);

        Event lastEvent = new Event();
        lastEvent.setTimestamp(Instant.now().minus(Duration.ofMinutes(5)));
        when(eventRepository.findFirstBySessionIdOrderByTimestampDesc(anyLong()))
                .thenReturn(Optional.of(lastEvent));

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(eventRepository, times(2)).existsBySessionIdAndType(anyLong(), eq(EventType.ERROR));
        verify(reportRepository, times(2)).save(any(Report.class));

        // Checking that both sessions were deactivated
        verify(session1).setIsActive(false);
        verify(session2).setIsActive(false);
        verify(session1).setEndTime(lastEvent.getTimestamp());
        verify(session2).setEndTime(lastEvent.getTimestamp());
    }

    @Test
    void handleExpiredSessions_ShouldSetSessionInactive() {
        // Arrange
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);

        List<Session> expiredSessions = List.of(session);
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(expiredSessions);

        when(eventRepository.existsBySessionIdAndType(1L, EventType.ERROR))
                .thenReturn(false);

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(session).setIsActive(false); // Проверяем, что isActive = false
    }

    @Test
    void handleExpiredSessions_ShouldSetEndTimeFromLastEvent() {
        // Arrange
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(1L);

        List<Session> expiredSessions = List.of(session);
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(expiredSessions);

        when(eventRepository.existsBySessionIdAndType(1L, EventType.ERROR))
                .thenReturn(false);

        Event lastEvent = new Event();
        Instant expectedEndTime = Instant.now().minus(Duration.ofMinutes(5));
        lastEvent.setTimestamp(expectedEndTime);
        when(eventRepository.findFirstBySessionIdOrderByTimestampDesc(1L))
                .thenReturn(Optional.of(lastEvent));

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(session).setEndTime(expectedEndTime);
    }

    @Test
    void handleExpiredSessions_WithEmptyList_ShouldDoNothing() {
        // Arrange
        when(sessionRepository.findExpiredSessions(any(Instant.class)))
                .thenReturn(List.of());

        // Act
        job.handleExpiredSessions();

        // Assert
        verify(eventRepository, never()).existsBySessionIdAndType(anyLong(), any());
        verify(eventRepository, never()).deleteBySessionId(anyLong());
        verify(reportRepository, never()).save(any());
    }
}
package uni.bugtracker.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uni.bugtracker.backend.dto.event.EventRequest;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.Event;
import uni.bugtracker.backend.model.EventType;
import uni.bugtracker.backend.model.Session;
import uni.bugtracker.backend.model.Project;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.SessionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private EventService eventService;

    private EventRequest eventRequest;
    private Session session;
    private Event savedEvent;

    @BeforeEach
    void setUp() {
        // Setup test data
        Project project = new Project();
        project.setId("project-123");

        session = new Session();
        session.setId(1L);
        session.setProject(project);

        EventRequest.MetadataPart metadata = new EventRequest.MetadataPart();
        metadata.setFileName("app.js");
        metadata.setLineNumber("42");
        metadata.setStatusCode("404");

        eventRequest = new EventRequest();
        eventRequest.setSessionId(1L);
        eventRequest.setType(EventType.ERROR);
        eventRequest.setName("Test Event");
        eventRequest.setLog("Error log");
        eventRequest.setStackTrace("Stack trace");
        eventRequest.setUrl("http://example.com");
        eventRequest.setElement("#button");
        eventRequest.setTimestamp(Instant.now());
        eventRequest.setMetadata(metadata);

        savedEvent = new Event();
        savedEvent.setId(100L);
        savedEvent.setSession(session);
        savedEvent.setType(EventType.ERROR);
        savedEvent.setName("Test Event");
        savedEvent.setLog("Error log");
        savedEvent.setStackTrace("Stack trace");
        savedEvent.setUrl("http://example.com");
        savedEvent.setElement("#button");
        savedEvent.setTimestamp(eventRequest.getTimestamp());

        Event.Metadata eventMetadata = new Event.Metadata();
        eventMetadata.setFileName("app.js");
        eventMetadata.setLineNumber("42");
        eventMetadata.setStatusCode("404");
        savedEvent.setMetadata(eventMetadata);
    }

    @Test
    void createEvent_shouldSaveEventAndReturnId() {
        // Given
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // When
        Long eventId = eventService.createEvent(eventRequest);

        // Then
        assertThat(eventId).isEqualTo(100L);
        verify(sessionRepository).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_shouldTrimLongFields() {
        // Given
        String longLog = "x".repeat(5_000_000);
        eventRequest.setLog(longLog);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            assertThat(event.getLog()).hasSize(4_000_000);
            return savedEvent;
        });

        // When
        eventService.createEvent(eventRequest);

        // Then - логика трима проверена в моке
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_whenSessionNotFound_shouldThrowException() {
        // Given
        when(sessionRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.createEvent(eventRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void createEvent_withoutMetadata_shouldCreateEventWithoutMetadata() {
        // Given
        eventRequest.setMetadata(null);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            assertThat(event.getMetadata()).isNull();
            return savedEvent;
        });

        // When
        eventService.createEvent(eventRequest);

        // Then
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void getEvent_shouldReturnEventDetails() {
        // Given
        when(eventRepository.findById(100L)).thenReturn(Optional.of(savedEvent));

        // When
        var result = eventService.getEvent(100L);

        // Then
        assertThat(result.getEventId()).isEqualTo(100L);
        assertThat(result.getSessionId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(EventType.ERROR);
        verify(eventRepository).findById(100L);
    }

    @Test
    void getEvent_whenEventNotFound_shouldThrowException() {
        // Given
        when(eventRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> eventService.getEvent(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void getEventsBySession_shouldReturnEventsForSession() {
        // Given
        List<Event> events = List.of(savedEvent);
        when(eventRepository.findAllBySessionId(1L)).thenReturn(events);

        // When
        var result = eventService.getEventsBySession(1L);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getEventId()).isEqualTo(100L);
        verify(eventRepository).findAllBySessionId(1L);
    }

    @Test
    void getEventsBySession_whenNoEvents_shouldReturnEmptyList() {
        // Given
        when(eventRepository.findAllBySessionId(1L)).thenReturn(List.of());

        // When
        var result = eventService.getEventsBySession(1L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void getProjectIdByEventId_shouldReturnProjectId() {
        // Given
        when(eventRepository.findById(100L)).thenReturn(Optional.of(savedEvent));
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        // When
        String projectId = eventService.getProjectIdByEventId(100L);

        // Then
        assertThat(projectId).isEqualTo("project-123");
    }

    @Test
    void trim_shouldReturnNullForNullInput() {
        // When & Then - using reflection to test private method
        // Можно протестировать через публичные методы, которые используют trim
        eventRequest.setLog(null);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            assertThat(event.getLog()).isNull();
            return savedEvent;
        });

        eventService.createEvent(eventRequest);
    }
}
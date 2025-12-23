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
import uni.bugtracker.backend.model.Session;
import uni.bugtracker.backend.repository.EventRepository;
import uni.bugtracker.backend.repository.SessionRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private EventService eventService;

    private Session session;
    private EventRequest eventRequest;

    @BeforeEach
    void setUp() {
        session = new Session();
        session.setId(1L);

        eventRequest = new EventRequest();
        eventRequest.setSessionId(1L);
        eventRequest.setType(uni.bugtracker.backend.model.EventType.ERROR);
        eventRequest.setName("Test Event");
        eventRequest.setLog("Test log message");
        eventRequest.setUrl("https://example.com");
        eventRequest.setTimestamp(Instant.now());
    }

    @Test
    void createEvent_WithValidRequest_ShouldSaveAndReturnId() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        Long eventId = eventService.createEvent(eventRequest);

        // Assert
        assertThat(eventId).isEqualTo(100L);
        verify(sessionRepository).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void createEvent_WithNonExistentSession_ShouldThrowException() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> eventService.createEvent(eventRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void createEvent_WithLongName_ShouldTrimName() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        String longName = "A".repeat(300); // More than 255 symbols
        eventRequest.setName(longName);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                event.getName() != null && event.getName().length() == 255
        ));
    }

    @Test
    void createEvent_WithLongLog_ShouldTrimLog() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        String longLog = "A".repeat(4_000_100); // More than limit
        eventRequest.setLog(longLog);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                event.getLog() != null && event.getLog().length() == 4_000_000
        ));
    }

    @Test
    void createEvent_WithLongStackTrace_ShouldTrimStackTrace() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        String longStackTrace = "A".repeat(4_000_100); // More than limit
        eventRequest.setStackTrace(longStackTrace);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                event.getStackTrace() != null && event.getStackTrace().length() == 4_000_000
        ));
    }

    @Test
    void createEvent_WithNullStackTrace_ShouldHandleGracefully() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        eventRequest.setStackTrace(null);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                event.getStackTrace() == null
        ));
    }

    @Test
    void createEvent_WithMetadata_ShouldSetMetadata() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        EventRequest.MetadataPart metadataPart = new EventRequest.MetadataPart();
        metadataPart.setFileName("app.js");
        metadataPart.setLineNumber("42");
        metadataPart.setStatusCode("404");
        eventRequest.setMetadata(metadataPart);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                event.getMetadata() != null &&
                        "app.js".equals(event.getMetadata().getFileName()) &&
                        "42".equals(event.getMetadata().getLineNumber()) &&
                        "404".equals(event.getMetadata().getStatusCode())
        ));
    }

    @Test
    void createEvent_WithoutMetadata_ShouldNotSetMetadata() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        eventRequest.setMetadata(null);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                event.getMetadata() == null
        ));
    }

    @Test
    void createEvent_WithAllFields_ShouldSetAllFields() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        Instant timestamp = Instant.now();
        eventRequest.setName("Test Event");
        eventRequest.setLog("Log message");
        eventRequest.setStackTrace("Stack trace");
        eventRequest.setUrl("https://example.com");
        eventRequest.setElement("#button");
        eventRequest.setTimestamp(timestamp);

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                "Test Event".equals(event.getName()) &&
                        "Log message".equals(event.getLog()) &&
                        "Stack trace".equals(event.getStackTrace()) &&
                        "https://example.com".equals(event.getUrl()) &&
                        "#button".equals(event.getElement()) &&
                        timestamp.equals(event.getTimestamp())
        ));
    }

    @Test
    void createEvent_WithEmptyStringFields_ShouldHandleGracefully() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        eventRequest.setName("");
        eventRequest.setLog("");
        eventRequest.setUrl("");

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                "".equals(event.getName()) &&
                        "".equals(event.getLog()) &&
                        "".equals(event.getUrl())
        ));
    }

    @Test
    void createEvent_WithElement_ShouldSetElement() {
        // Arrange
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        eventRequest.setElement("#submit-button");

        Event savedEvent = new Event();
        savedEvent.setId(100L);
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        // Act
        eventService.createEvent(eventRequest);

        // Assert
        verify(eventRepository).save(argThat(event ->
                "#submit-button".equals(event.getElement())
        ));
    }
}
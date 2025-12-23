package uni.bugtracker.backend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uni.bugtracker.backend.dto.event.EventDetailsResponse;
import uni.bugtracker.backend.dto.event.EventRequest;
import uni.bugtracker.backend.exception.ResourceNotFoundException;
import uni.bugtracker.backend.model.EventType;
import uni.bugtracker.backend.service.EventService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@AutoConfigureJsonTesters
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<EventRequest> eventRequestJson;

    @MockitoBean
    private EventService eventService;

    private EventRequest eventRequest;
    private EventDetailsResponse mockEventDetailsResponse;
    private EventDetailsResponse.Metadata mockMetadata;

    @BeforeEach
    void setUp() {
        EventRequest.MetadataPart metadata = new EventRequest.MetadataPart();
        metadata.setFileName("app.js");
        metadata.setLineNumber("42");
        metadata.setStatusCode("404");

        eventRequest = new EventRequest();
        eventRequest.setSessionId(1L);
        eventRequest.setType(EventType.ERROR);
        eventRequest.setName("Test Error");
        eventRequest.setLog("Error occurred while processing");
        eventRequest.setStackTrace("at app.js:42");
        eventRequest.setUrl("http://example.com");
        eventRequest.setElement("#submit-button");
        eventRequest.setTimestamp(Instant.now());
        eventRequest.setMetadata(metadata);

        // Создаем mock EventDetailsResponse и Metadata
        mockEventDetailsResponse = mock(EventDetailsResponse.class);
        mockMetadata = mock(EventDetailsResponse.Metadata.class);

        when(mockEventDetailsResponse.getEventId()).thenReturn(1L);
        when(mockEventDetailsResponse.getSessionId()).thenReturn(1L);
        when(mockEventDetailsResponse.getType()).thenReturn(EventType.ERROR);
        when(mockEventDetailsResponse.getName()).thenReturn("Test Error");
        when(mockEventDetailsResponse.getLog()).thenReturn("Error occurred while processing");
        when(mockEventDetailsResponse.getStackTrace()).thenReturn("at app.js:42");
        when(mockEventDetailsResponse.getUrl()).thenReturn("http://example.com");
        when(mockEventDetailsResponse.getElement()).thenReturn("#submit-button");
        when(mockEventDetailsResponse.getTimestamp()).thenReturn(Instant.now());
        when(mockEventDetailsResponse.getMetadata()).thenReturn(mockMetadata);

        when(mockMetadata.getFileName()).thenReturn("app.js");
        when(mockMetadata.getLineNumber()).thenReturn("42");
        when(mockMetadata.getStatusCode()).thenReturn("404");
    }

    @Test
    void createEvent_ShouldReturnCreated() throws Exception {
        // Arrange
        Long expectedEventId = 1L;
        when(eventService.createEvent(any(EventRequest.class)))
                .thenReturn(expectedEventId);

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJson.write(eventRequest).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Event created successfully"))
                .andExpect(jsonPath("$.eventId").value(expectedEventId));
    }

    @Test
    void createEvent_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        // Arrange
        eventRequest.setSessionId(null);

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJson.write(eventRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvent_ShouldReturnOk() throws Exception {
        // Arrange
        Long eventId = 1L;
        when(eventService.getEvent(eventId))
                .thenReturn(mockEventDetailsResponse);

        // Act & Assert
        mockMvc.perform(get("/api/events/{id}", eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(eventId))
                .andExpect(jsonPath("$.type").value("ERROR"))
                .andExpect(jsonPath("$.metadata.fileName").value("app.js"));
    }

    @Test
    void getEventsBySession_ShouldReturnOk() throws Exception {
        // Arrange
        Long sessionId = 1L;
        List<EventDetailsResponse> events = List.of(mockEventDetailsResponse);
        when(eventService.getEventsBySession(sessionId))
                .thenReturn(events);

        // Act & Assert
        mockMvc.perform(get("/api/events/session/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$[0].name").value("Test Error"));
    }

    @Test
    void getEventsBySession_WhenNoEvents_ShouldReturnNoContent() throws Exception {
        // Arrange
        Long sessionId = 1L;
        when(eventService.getEventsBySession(sessionId))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/events/session/{sessionId}", sessionId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createEvent_WithInvalidMetadata_ShouldReturnBadRequest() throws Exception {
        // Arrange
        eventRequest.getMetadata().setLineNumber("abc");
        eventRequest.getMetadata().setStatusCode("404abc");

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJson.write(eventRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_WithFutureTimestamp_ShouldReturnBadRequest() throws Exception {
        // Arrange
        eventRequest.setTimestamp(Instant.now().plusSeconds(3600));

        // Act & Assert
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(eventRequestJson.write(eventRequest).getJson()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvent_WithNonExistentId_ShouldReturnNotFound() throws Exception {
        // Arrange
        Long nonExistentId = 999L;
        when(eventService.getEvent(nonExistentId))
                .thenThrow(new ResourceNotFoundException("Event not found"));

        // Act & Assert
        mockMvc.perform(get("/api/events/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

}